import Fastify, { FastifyInstance, FastifyServerOptions } from 'fastify';
import fastifyJwt from '@fastify/jwt';
import fastifyCookie from '@fastify/cookie';
import fastifyMultipart from '@fastify/multipart';
import authRoutes from './routes/auth';
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

  return app;
}
