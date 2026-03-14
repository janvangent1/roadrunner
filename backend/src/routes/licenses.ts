import fp from 'fastify-plugin';
import { FastifyInstance, FastifyReply } from 'fastify';
import { z } from 'zod';
import { prisma } from '../lib/prisma';
import { redisGet, redisSet } from '../lib/redis';
import { requireAuth } from '../middleware/requireAuth';

const CACHE_TTL_SECONDS = 60;

const checkLicenseSchema = z.object({
  routeId: z.string().uuid(),
  deviceId: z.string(), // Reserved for v2 Play Integrity binding — stored but not validated
});

/** Shape of the cached license validity record. */
interface LicenseCacheEntry {
  valid: boolean;
  expiresAt: string | null;
  licenseType?: string;
}

async function licenseHandlersPlugin(fastify: FastifyInstance): Promise<void> {
  /**
   * POST /api/v1/licenses/check
   * Validate a license server-side before navigation.
   *
   * Uses Redis for 60s caching and the server clock (new Date()) for expiry
   * validation — device time is NEVER trusted.
   */
  fastify.post('/check', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const parseResult = checkLicenseSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: parseResult.error.errors });
    }

    const { routeId } = parseResult.data;
    const userId = (request.user as { sub: string }).sub;
    const cacheKey = `license:${userId}:${routeId}`;

    // Step 1: Redis cache check
    const cached = await redisGet(cacheKey);
    if (cached !== null) {
      let entry: LicenseCacheEntry;
      try {
        entry = JSON.parse(cached) as LicenseCacheEntry;
      } catch {
        // Corrupt cache entry — fall through to DB query
        entry = { valid: false, expiresAt: null };
      }

      if (!entry.valid) {
        return reply.code(403).send({ error: 'No valid license', code: 'LICENSE_INVALID' });
      }

      // Cache hit with valid = true — skip DB query and issue session JWT
      return buildSessionResponse(fastify, reply, userId, routeId, entry.expiresAt, entry.licenseType);
    }

    // Step 2: DB validation — server clock only, never device time
    const license = await prisma.license.findFirst({
      where: {
        userId,
        routeId,
        revokedAt: null,
        OR: [
          { expiresAt: null },                       // PERMANENT
          { expiresAt: { gt: new Date() } },         // Not yet expired (server clock)
        ],
      },
      select: {
        type: true,
        expiresAt: true,
      },
    });

    if (!license) {
      // Cache the negative result so rapid re-checks don't hammer the DB
      await redisSet(cacheKey, JSON.stringify({ valid: false, expiresAt: null }), CACHE_TTL_SECONDS);
      return reply.code(403).send({ error: 'No valid license', code: 'LICENSE_INVALID' });
    }

    // Cache the positive result
    const expiresAtIso = license.expiresAt ? license.expiresAt.toISOString() : null;
    await redisSet(
      cacheKey,
      JSON.stringify({ valid: true, expiresAt: expiresAtIso, licenseType: license.type }),
      CACHE_TTL_SECONDS,
    );

    return buildSessionResponse(fastify, reply, userId, routeId, expiresAtIso, license.type);
  });
}

/**
 * Sign a 1-hour navigation session JWT and send the 200 response.
 * All timestamps are derived from the server clock (Date.now()), never device time.
 */
function buildSessionResponse(
  fastify: FastifyInstance,
  reply: FastifyReply,
  userId: string,
  routeId: string,
  licenseExpiresAt: string | null,
  licenseType?: string,
): FastifyReply {
  // 1-hour session window from server clock — device time is never used
  const sessionExpiresAt = new Date(Date.now() + 60 * 60 * 1000);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const sessionToken = (fastify.jwt.sign as any)(
    {
      sub: userId,
      routeId,
      sessionType: 'navigation',
      issuedAt: new Date().toISOString(), // Server clock
      sessionExpiresAt: sessionExpiresAt.toISOString(),
    },
    { expiresIn: '1h' },
  ) as string;

  return reply.send({
    valid: true,
    sessionToken,
    sessionExpiresAt: sessionExpiresAt.toISOString(),
    licenseType: licenseType ?? null,
    licenseExpiresAt,
  });
}

export const licenseHandlers = fp(licenseHandlersPlugin);
export default licenseHandlers;
