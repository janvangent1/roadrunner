import { FastifyRequest, FastifyReply } from 'fastify';

export async function requireAdmin(request: FastifyRequest, reply: FastifyReply): Promise<void> {
  if (request.user.role !== 'ADMIN') {
    reply.code(403).send({ error: 'Forbidden' });
  }
}
