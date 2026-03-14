import fp from 'fastify-plugin';
import { FastifyInstance } from 'fastify';
import { Difficulty, WaypointType } from '@prisma/client';
import { prisma } from '../lib/prisma';
import { redis } from '../lib/redis';
import { requireAuth } from '../middleware/requireAuth';
import { requireAdmin } from '../middleware/requireAdmin';
import { encryptGpx } from '../lib/tink';

const CATALOG_CACHE_KEY = 'catalog';

/** Valid Difficulty values for request validation */
const VALID_DIFFICULTIES = Object.values(Difficulty) as string[];

/** Valid WaypointType values for request validation */
const VALID_WAYPOINT_TYPES = Object.values(WaypointType) as string[];

/**
 * Collect all bytes from a readable stream into a Buffer.
 * Never writes to disk — all data is accumulated in memory.
 */
async function streamToBuffer(stream: NodeJS.ReadableStream): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    stream.on('data', (chunk: Buffer | string) => {
      chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
    });
    stream.on('end', () => resolve(Buffer.concat(chunks)));
    stream.on('error', reject);
  });
}

async function adminRouteHandlersPlugin(fastify: FastifyInstance): Promise<void> {
  /**
   * POST /api/v1/admin/routes
   * Upload a new route with a GPX file (multipart/form-data).
   * The GPX is encrypted in memory with Tink before being stored.
   * Plaintext GPX is never written to disk or stored in the database.
   */
  fastify.post('/', {
    preHandler: [requireAuth, requireAdmin],
  }, async (request, reply) => {
    const data = await request.file();
    if (!data) {
      return reply.code(400).send({ error: 'No file uploaded — expected multipart/form-data with gpx field' });
    }

    // Read all file bytes into memory — NEVER written to disk
    const gpxBuffer = await streamToBuffer(data.file);

    // Collect remaining form fields
    const fields = data.fields as Record<string, { value: string } | undefined>;

    const title = fields['title']?.value;
    const description = fields['description']?.value ?? null;
    const difficulty = fields['difficulty']?.value;
    const terrainType = fields['terrainType']?.value;
    const region = fields['region']?.value;
    const estimatedDurationMinutesStr = fields['estimatedDurationMinutes']?.value;
    const distanceKmStr = fields['distanceKm']?.value;
    const waypointsStr = fields['waypoints']?.value;

    // Validate required fields
    if (!title || !difficulty || !terrainType || !region || !estimatedDurationMinutesStr || !distanceKmStr) {
      return reply.code(400).send({
        error: 'Missing required fields: title, difficulty, terrainType, region, estimatedDurationMinutes, distanceKm',
      });
    }

    if (!VALID_DIFFICULTIES.includes(difficulty)) {
      return reply.code(400).send({ error: `Invalid difficulty. Must be one of: ${VALID_DIFFICULTIES.join(', ')}` });
    }

    const estimatedDurationMinutes = parseInt(estimatedDurationMinutesStr, 10);
    const distanceKm = parseFloat(distanceKmStr);

    if (isNaN(estimatedDurationMinutes) || isNaN(distanceKm)) {
      return reply.code(400).send({ error: 'estimatedDurationMinutes must be an integer, distanceKm must be a number' });
    }

    // Parse waypoints if provided
    let waypointsData: Array<{
      label: string;
      latitude: number;
      longitude: number;
      type: string;
      sortOrder: number;
    }> | undefined;

    if (waypointsStr) {
      try {
        const parsed = JSON.parse(waypointsStr) as unknown[];
        if (!Array.isArray(parsed)) {
          return reply.code(400).send({ error: 'waypoints must be a JSON array' });
        }
        // Validate each waypoint
        for (const wp of parsed) {
          const w = wp as Record<string, unknown>;
          if (!w['label'] || !w['latitude'] || !w['longitude'] || !w['type'] || w['sortOrder'] === undefined) {
            return reply.code(400).send({ error: 'Each waypoint must have label, latitude, longitude, type, sortOrder' });
          }
          if (!VALID_WAYPOINT_TYPES.includes(w['type'] as string)) {
            return reply.code(400).send({ error: `Invalid waypoint type. Must be one of: ${VALID_WAYPOINT_TYPES.join(', ')}` });
          }
        }
        waypointsData = parsed as typeof waypointsData;
      } catch {
        return reply.code(400).send({ error: 'waypoints must be valid JSON' });
      }
    }

    // We need a route ID for AAD before creating the route.
    // Generate the ID here so it can be used as AAD for encryption.
    const { v4: uuidv4 } = await import('uuid');
    const routeId = uuidv4();

    // Encrypt GPX in memory — plaintext never written to disk or stored
    const gpxEncrypted = await encryptGpx(gpxBuffer, Buffer.from(routeId));

    // Create route (and waypoints) in a single transaction
    const route = await prisma.$transaction(async (tx) => {
      const created = await tx.route.create({
        data: {
          id: routeId,
          title,
          description,
          difficulty: difficulty as Difficulty,
          terrainType,
          region,
          estimatedDurationMinutes,
          distanceKm,
          published: false,
          gpxEncrypted,
          ...(waypointsData && {
            waypoints: {
              create: waypointsData.map((w) => ({
                label: w.label,
                latitude: w.latitude,
                longitude: w.longitude,
                type: w.type as WaypointType,
                sortOrder: w.sortOrder,
              })),
            },
          }),
        },
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
          createdAt: true,
          updatedAt: true,
          // gpxEncrypted intentionally excluded from response
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
      return created;
    });

    // Invalidate catalog cache
    await redis.del(CATALOG_CACHE_KEY);

    return reply.code(201).send({
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
   * PATCH /api/v1/admin/routes/:id
   * Update route metadata and optionally replace the GPX file.
   * Accepts JSON body for metadata-only updates, or multipart for GPX replacement.
   */
  fastify.patch<{ Params: { id: string } }>('/:id', {
    preHandler: [requireAuth, requireAdmin],
  }, async (request, reply) => {
    const { id } = request.params;

    // Verify route exists
    const existing = await prisma.route.findUnique({
      where: { id },
      select: { id: true },
    });
    if (!existing) {
      return reply.code(404).send({ error: 'Route not found' });
    }

    const contentType = request.headers['content-type'] ?? '';
    let updateData: {
      title?: string;
      description?: string | null;
      difficulty?: Difficulty;
      terrainType?: string;
      region?: string;
      estimatedDurationMinutes?: number;
      distanceKm?: number;
      published?: boolean;
      gpxEncrypted?: Buffer;
    } = {};

    if (contentType.includes('multipart/form-data')) {
      // Multipart: may include a new GPX file
      const data = await request.file();
      if (data) {
        // New GPX file provided — read into memory and re-encrypt
        const gpxBuffer = await streamToBuffer(data.file);
        updateData.gpxEncrypted = await encryptGpx(gpxBuffer, Buffer.from(id));

        const fields = data.fields as Record<string, { value: string } | undefined>;
        if (fields['title']?.value) updateData.title = fields['title'].value;
        if (fields['description']?.value !== undefined) updateData.description = fields['description']?.value ?? null;
        if (fields['terrainType']?.value) updateData.terrainType = fields['terrainType'].value;
        if (fields['region']?.value) updateData.region = fields['region'].value;
        if (fields['estimatedDurationMinutes']?.value) {
          updateData.estimatedDurationMinutes = parseInt(fields['estimatedDurationMinutes'].value, 10);
        }
        if (fields['distanceKm']?.value) {
          updateData.distanceKm = parseFloat(fields['distanceKm'].value);
        }
        if (fields['published']?.value !== undefined) {
          updateData.published = fields['published'].value === 'true';
        }
        if (fields['difficulty']?.value) {
          const diff = fields['difficulty'].value;
          if (!VALID_DIFFICULTIES.includes(diff)) {
            return reply.code(400).send({ error: `Invalid difficulty. Must be one of: ${VALID_DIFFICULTIES.join(', ')}` });
          }
          updateData.difficulty = diff as Difficulty;
        }
      }
    } else {
      // JSON body: metadata-only update
      const body = request.body as Record<string, unknown>;
      if (body['title'] !== undefined) updateData.title = body['title'] as string;
      if (body['description'] !== undefined) updateData.description = (body['description'] as string | null) ?? null;
      if (body['terrainType'] !== undefined) updateData.terrainType = body['terrainType'] as string;
      if (body['region'] !== undefined) updateData.region = body['region'] as string;
      if (body['estimatedDurationMinutes'] !== undefined) {
        updateData.estimatedDurationMinutes = body['estimatedDurationMinutes'] as number;
      }
      if (body['distanceKm'] !== undefined) updateData.distanceKm = body['distanceKm'] as number;
      if (body['published'] !== undefined) updateData.published = body['published'] as boolean;
      if (body['difficulty'] !== undefined) {
        const diff = body['difficulty'] as string;
        if (!VALID_DIFFICULTIES.includes(diff)) {
          return reply.code(400).send({ error: `Invalid difficulty. Must be one of: ${VALID_DIFFICULTIES.join(', ')}` });
        }
        updateData.difficulty = diff as Difficulty;
      }
    }

    const updated = await prisma.route.update({
      where: { id },
      data: updateData,
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
        createdAt: true,
        updatedAt: true,
        // gpxEncrypted intentionally excluded
      },
    });

    // Invalidate catalog cache
    await redis.del(CATALOG_CACHE_KEY);

    return reply.send({
      ...updated,
      distanceKm: Number(updated.distanceKm),
    });
  });

  /**
   * DELETE /api/v1/admin/routes/:id
   * Delete a route and all its waypoints (cascade per schema).
   */
  fastify.delete<{ Params: { id: string } }>('/:id', {
    preHandler: [requireAuth, requireAdmin],
  }, async (request, reply) => {
    const { id } = request.params;

    const existing = await prisma.route.findUnique({
      where: { id },
      select: { id: true },
    });
    if (!existing) {
      return reply.code(404).send({ error: 'Route not found' });
    }

    await prisma.route.delete({ where: { id } });

    // Invalidate catalog cache
    await redis.del(CATALOG_CACHE_KEY);

    return reply.code(204).send();
  });
}

export const adminRouteHandlers = fp(adminRouteHandlersPlugin);
export default adminRouteHandlers;
