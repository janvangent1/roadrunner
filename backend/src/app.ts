import Fastify, { FastifyInstance, FastifyServerOptions } from 'fastify';
import fastifyJwt from '@fastify/jwt';
import fastifyCookie from '@fastify/cookie';
import fastifyMultipart from '@fastify/multipart';
import authRoutes from './routes/auth';
import { routeHandlers } from './routes/routes';
import { adminRouteHandlers } from './routes/adminRoutes';
import { licenseHandlers } from './routes/licenses';
import { adminLicenseHandlers } from './routes/adminLicenses';
import { prisma } from './lib/prisma';
import { redis } from './lib/redis';

export async function buildApp(opts: FastifyServerOptions = {}): Promise<FastifyInstance> {
  const app = Fastify({
    logger: true,
    ...opts,
  });

  // Register plugins
  await app.register(fastifyJwt, {
    secret: process.env.JWT_SECRET || (() => { throw new Error('JWT_SECRET env var is required'); })(),
  });

  await app.register(fastifyCookie);

  await app.register(fastifyMultipart, {
    limits: {
      fileSize: 50 * 1024 * 1024, // 50 MB
    },
  });

  // Graceful shutdown: disconnect Prisma and Redis on SIGTERM
  app.addHook('onClose', async () => {
    await prisma.$disconnect();
    await redis.quit();
  });

  // Health check route
  app.get('/health', async (_request, _reply) => {
    return { status: 'ok', timestamp: new Date().toISOString() };
  });

  // Auth routes
  await app.register(authRoutes, { prefix: '/api/v1/auth' });

  // Route catalog and GPX endpoints
  await app.register(routeHandlers, { prefix: '/api/v1/routes' });

  // Admin route management endpoints
  await app.register(adminRouteHandlers, { prefix: '/api/v1/admin/routes' });

  // License check endpoint (navigation gating)
  await app.register(licenseHandlers, { prefix: '/api/v1/licenses' });

  // Admin license management endpoints
  await app.register(adminLicenseHandlers, { prefix: '/api/v1/admin/licenses' });

  return app;
}
