import Fastify, { FastifyInstance, FastifyServerOptions } from 'fastify';
import fastifyJwt from '@fastify/jwt';
import fastifyCookie from '@fastify/cookie';
import fastifyMultipart from '@fastify/multipart';

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

  // Health check route
  app.get('/health', async (_request, _reply) => {
    return { status: 'ok', timestamp: new Date().toISOString() };
  });

  // Route handlers will be registered here via app.register() in subsequent plans

  return app;
}
