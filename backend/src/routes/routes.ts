import { FastifyInstance } from 'fastify';
import { prisma } from '../lib/prisma';
import { redisGet, redisSet } from '../lib/redis';
import { requireAuth } from '../middleware/requireAuth';

const CATALOG_CACHE_KEY = 'catalog';
const CATALOG_CACHE_TTL = 300; // 5 minutes

async function routeHandlersPlugin(fastify: FastifyInstance): Promise<void> {
  /**
   * GET /api/v1/routes
   * Returns array of published routes (metadata only — no GPX bytes).
   * Results are cached in Redis for 5 minutes.
   */
  fastify.get('/', {
    preHandler: [requireAuth],
  }, async (_request, reply) => {
    // Check Redis cache first
    const cached = await redisGet(CATALOG_CACHE_KEY);
    if (cached) {
      return reply.send(JSON.parse(cached));
    }

    const routes = await prisma.route.findMany({
      where: { published: true },
      select: {
        id: true,
        title: true,
        description: true,
        difficulty: true,
        terrainType: true,
        region: true,
        estimatedDurationMinutes: true,
        distanceKm: true,
        published: true,
        centerLat: true,
        centerLng: true,
        createdAt: true,
        updatedAt: true,
        // gpxEncrypted intentionally excluded
      },
    });

    const result = routes.map((r) => ({
      ...r,
      distanceKm: Number(r.distanceKm),
    }));

    await redisSet(CATALOG_CACHE_KEY, JSON.stringify(result), CATALOG_CACHE_TTL);

    return reply.send(result);
  });

  /**
   * GET /api/v1/routes/:id
   * Returns a single route with metadata and waypoints (no GPX bytes).
   */
  fastify.get<{ Params: { id: string } }>('/:id', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { id } = request.params;

    const route = await prisma.route.findUnique({
      where: { id },
      select: {
        id: true,
        title: true,
        description: true,
        difficulty: true,
        terrainType: true,
        region: true,
        estimatedDurationMinutes: true,
        distanceKm: true,
        published: true,
        centerLat: true,
        centerLng: true,
        createdAt: true,
        updatedAt: true,
        // gpxEncrypted intentionally excluded
        waypoints: {
          orderBy: { sortOrder: 'asc' },
          select: {
            id: true,
            label: true,
            latitude: true,
            longitude: true,
            type: true,
            sortOrder: true,
          },
        },
      },
    });

    if (!route) {
      return reply.code(404).send({ error: 'Route not found' });
    }

    return reply.send({
      ...route,
      distanceKm: Number(route.distanceKm),
      waypoints: route.waypoints.map((w) => ({
        ...w,
        latitude: Number(w.latitude),
        longitude: Number(w.longitude),
      })),
    });
  });

  /**
   * GET /api/v1/routes/:id/gpx
   * Returns the encrypted GPX bytes for a user with a valid license.
   * Requires auth (requireAuth). License check enforced server-side.
   */
  fastify.get<{ Params: { id: string } }>('/:id/gpx', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { id } = request.params;
    const userId = request.user.sub;

    // License check — server side only, device clock is never trusted
    const license = await prisma.license.findFirst({
      where: {
        userId,
        routeId: id,
        revokedAt: null,
        AND: [
          {
            OR: [
              { expiresAt: null },
              { expiresAt: { gt: new Date() } },
            ],
          },
        ],
      },
    });

    if (!license) {
      return reply.code(403).send({ error: 'No valid license for this route' });
    }

    const route = await prisma.route.findUnique({
      where: { id },
      select: { gpxEncrypted: true },
    });

    if (!route || !route.gpxEncrypted) {
      return reply.code(404).send({ error: 'Route or GPX not found' });
    }

    return reply
      .code(200)
      .header('Content-Type', 'application/octet-stream')
      .header('X-Encrypted', 'tink-aes256gcm')
      .send(Buffer.from(route.gpxEncrypted));
  });
}

export const routeHandlers = routeHandlersPlugin;
export default routeHandlers;
