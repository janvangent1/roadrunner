import fp from 'fastify-plugin';
import { FastifyInstance } from 'fastify';
import { LicenseType } from '@prisma/client';
import { z } from 'zod';
import { prisma } from '../lib/prisma';
import { redis } from '../lib/redis';
import { requireAuth } from '../middleware/requireAuth';
import { requireAdmin } from '../middleware/requireAdmin';

/** Valid LicenseType values */
const VALID_LICENSE_TYPES = Object.values(LicenseType) as string[];

const grantLicenseSchema = z.object({
  email: z.string().email(),
  routeId: z.string().uuid(),
  type: z.enum(['DAY_PASS', 'MULTI_DAY', 'PERMANENT']),
  expiresAt: z.string().nullable(),
});

const modifyLicenseSchema = z
  .object({
    type: z.enum(['DAY_PASS', 'MULTI_DAY', 'PERMANENT']).optional(),
    expiresAt: z.string().nullable().optional(),
    revoked: z.boolean().optional(),
  })
  .refine((data) => Object.keys(data).length > 0, {
    message: 'At least one field must be provided',
  });

async function adminLicenseHandlersPlugin(fastify: FastifyInstance): Promise<void> {
  /**
   * POST /api/v1/admin/licenses
   * Grant a license to a user by email for a specific route.
   */
  fastify.post('/', {
    preHandler: [requireAuth, requireAdmin],
  }, async (request, reply) => {
    const parseResult = grantLicenseSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: parseResult.error.errors });
    }

    const { email, routeId, type, expiresAt } = parseResult.data;

    // Validate expiresAt is required for DAY_PASS and MULTI_DAY
    if ((type === 'DAY_PASS' || type === 'MULTI_DAY') && !expiresAt) {
      return reply.code(400).send({ error: 'expiresAt is required for DAY_PASS and MULTI_DAY license types' });
    }

    // Find user by email
    const user = await prisma.user.findUnique({
      where: { email },
      select: { id: true, email: true },
    });
    if (!user) {
      return reply.code(404).send({ error: 'User not found' });
    }

    // Verify route exists
    const route = await prisma.route.findUnique({
      where: { id: routeId },
      select: { id: true, title: true },
    });
    if (!route) {
      return reply.code(404).send({ error: 'Route not found' });
    }

    // Create the license
    const license = await prisma.license.create({
      data: {
        userId: user.id,
        routeId,
        type: type as LicenseType,
        expiresAt: expiresAt ? new Date(expiresAt) : null,
        revokedAt: null,
        linkedPurchaseToken: null,
      },
    });

    // Invalidate Redis cache for this user+route combination
    await redis.del(`license:${user.id}:${routeId}`);

    return reply.code(201).send({
      ...license,
      userEmail: user.email,
      routeTitle: route.title,
    });
  });

  /**
   * PATCH /api/v1/admin/licenses/:id
   * Revoke or modify an existing license (extend expiry, change type, set revokedAt).
   */
  fastify.patch<{ Params: { id: string } }>('/:id', {
    preHandler: [requireAuth, requireAdmin],
  }, async (request, reply) => {
    const { id } = request.params;

    const parseResult = modifyLicenseSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: parseResult.error.errors });
    }

    // Find the existing license
    const existing = await prisma.license.findUnique({
      where: { id },
      select: { id: true, userId: true, routeId: true },
    });
    if (!existing) {
      return reply.code(404).send({ error: 'License not found' });
    }

    const updates = parseResult.data;

    // Build update data
    const updateData: {
      type?: LicenseType;
      expiresAt?: Date | null;
      revokedAt?: Date | null;
    } = {};

    if (updates.type !== undefined) {
      updateData.type = updates.type as LicenseType;
    }
    if (updates.expiresAt !== undefined) {
      updateData.expiresAt = updates.expiresAt ? new Date(updates.expiresAt) : null;
    }
    if (updates.revoked !== undefined) {
      // Server clock — never trust device time
      updateData.revokedAt = updates.revoked ? new Date() : null;
    }

    const updated = await prisma.license.update({
      where: { id },
      data: updateData,
    });

    // Invalidate Redis cache — critical: revocation must propagate within 60s per cache TTL
    await redis.del(`license:${existing.userId}:${existing.routeId}`);

    return reply.send(updated);
  });

  /**
   * GET /api/v1/admin/licenses
   * List all licenses with optional filters by routeId or userId.
   */
  fastify.get('/', {
    preHandler: [requireAuth, requireAdmin],
  }, async (request, reply) => {
    const query = request.query as Record<string, string | undefined>;
    const routeId = query['routeId'];
    const userId = query['userId'];

    const licenses = await prisma.license.findMany({
      where: {
        ...(routeId ? { routeId } : {}),
        ...(userId ? { userId } : {}),
      },
      include: {
        user: { select: { id: true, email: true } },
        route: { select: { id: true, title: true } },
      },
      orderBy: { createdAt: 'desc' },
    });

    return reply.send(licenses);
  });
}

export const adminLicenseHandlers = fp(adminLicenseHandlersPlugin);
export default adminLicenseHandlers;
