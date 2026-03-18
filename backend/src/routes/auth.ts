import { FastifyInstance } from 'fastify';
import bcrypt from 'bcrypt';
import { v4 as uuidv4 } from 'uuid';
import { OAuth2Client } from 'google-auth-library';
import { z } from 'zod';
import { prisma } from '../lib/prisma';
import { requireAuth } from '../middleware/requireAuth';

const SALT_ROUNDS = 12;
const ACCESS_TOKEN_TTL = '15m';
const REFRESH_TOKEN_TTL_DAYS = 7;

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

// Zod schemas
const registerSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string(),
});

const googleSchema = z.object({
  idToken: z.string(),
});

const refreshSchema = z.object({
  refreshToken: z.string(),
});

const logoutSchema = z.object({
  refreshToken: z.string(),
});

async function generateTokens(
  fastify: FastifyInstance,
  userId: string,
  role: 'USER' | 'ADMIN',
): Promise<{ accessToken: string; refreshToken: string }> {
  // Sign access JWT
  const accessToken = fastify.jwt.sign(
    { sub: userId, role },
    { expiresIn: ACCESS_TOKEN_TTL },
  );

  // Generate a random refresh token (UUID), store hashed copy in DB
  const rawRefreshToken = uuidv4();
  const hashedRefreshToken = await bcrypt.hash(rawRefreshToken, SALT_ROUNDS);

  const expiresAt = new Date();
  expiresAt.setDate(expiresAt.getDate() + REFRESH_TOKEN_TTL_DAYS);

  await prisma.refreshToken.create({
    data: {
      token: hashedRefreshToken,
      userId,
      expiresAt,
    },
  });

  return { accessToken, refreshToken: rawRefreshToken };
}

async function authRoutes(fastify: FastifyInstance): Promise<void> {
  // POST /register
  fastify.post('/register', async (request, reply) => {
    const result = registerSchema.safeParse(request.body);
    if (!result.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: result.error.issues });
    }
    const { email, password } = result.data;

    // Check for existing user
    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) {
      return reply.code(409).send({ error: 'Email already in use' });
    }

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);

    const user = await prisma.user.create({
      data: { email, passwordHash, role: 'USER' },
    });

    const { accessToken, refreshToken } = await generateTokens(fastify, user.id, user.role);

    return reply.code(201).send({
      accessToken,
      refreshToken,
      user: { id: user.id, email: user.email, role: user.role },
    });
  });

  // POST /login
  fastify.post('/login', async (request, reply) => {
    const result = loginSchema.safeParse(request.body);
    if (!result.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: result.error.issues });
    }
    const { email, password } = result.data;

    const user = await prisma.user.findUnique({ where: { email } });
    if (!user || !user.passwordHash) {
      return reply.code(401).send({ error: 'Invalid credentials' });
    }

    const passwordMatch = await bcrypt.compare(password, user.passwordHash);
    if (!passwordMatch) {
      return reply.code(401).send({ error: 'Invalid credentials' });
    }

    const { accessToken, refreshToken } = await generateTokens(fastify, user.id, user.role);

    return reply.send({
      accessToken,
      refreshToken,
      user: { id: user.id, email: user.email, role: user.role },
    });
  });

  // POST /google
  fastify.post('/google', async (request, reply) => {
    const result = googleSchema.safeParse(request.body);
    if (!result.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: result.error.issues });
    }
    const { idToken } = result.data;

    let email: string;
    let googleSub: string;

    try {
      const ticket = await googleClient.verifyIdToken({
        idToken,
        audience: process.env.GOOGLE_CLIENT_ID,
      });
      const payload = ticket.getPayload();
      if (!payload || !payload.email || !payload.sub) {
        return reply.code(401).send({ error: 'Invalid Google token' });
      }
      email = payload.email;
      googleSub = payload.sub;
    } catch {
      return reply.code(401).send({ error: 'Google token verification failed' });
    }

    // Upsert: find by googleId first, then by email
    let user = await prisma.user.findFirst({ where: { googleId: googleSub } });

    if (!user) {
      const byEmail = await prisma.user.findUnique({ where: { email } });
      if (byEmail) {
        // Link Google account to existing email account
        user = await prisma.user.update({
          where: { id: byEmail.id },
          data: { googleId: googleSub },
        });
      } else {
        // Create new user
        user = await prisma.user.create({
          data: { email, googleId: googleSub, role: 'USER' },
        });
      }
    }

    const { accessToken, refreshToken } = await generateTokens(fastify, user.id, user.role);

    return reply.send({
      accessToken,
      refreshToken,
      user: { id: user.id, email: user.email, role: user.role },
    });
  });

  // POST /refresh
  fastify.post('/refresh', async (request, reply) => {
    const result = refreshSchema.safeParse(request.body);
    if (!result.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: result.error.issues });
    }
    const { refreshToken: rawToken } = result.data;

    // Find non-expired, non-revoked tokens to compare against
    const now = new Date();
    const candidates = await prisma.refreshToken.findMany({
      where: {
        revokedAt: null,
        expiresAt: { gt: now },
      },
      include: { user: true },
    });

    // Find the matching token by bcrypt comparison
    let matchedRecord: (typeof candidates)[number] | null = null;
    for (const candidate of candidates) {
      const isMatch = await bcrypt.compare(rawToken, candidate.token);
      if (isMatch) {
        matchedRecord = candidate;
        break;
      }
    }

    if (!matchedRecord) {
      return reply.code(401).send({ error: 'Invalid or expired refresh token' });
    }

    // Revoke old token
    await prisma.refreshToken.update({
      where: { id: matchedRecord.id },
      data: { revokedAt: now },
    });

    // Issue new tokens
    const { accessToken, refreshToken: newRefreshToken } = await generateTokens(
      fastify,
      matchedRecord.userId,
      matchedRecord.user.role,
    );

    return reply.send({ accessToken, refreshToken: newRefreshToken });
  });

  // POST /logout — requires auth
  fastify.post('/logout', {
    preHandler: requireAuth,
  }, async (request, reply) => {
    const result = logoutSchema.safeParse(request.body);
    if (!result.success) {
      return reply.code(400).send({ error: 'Invalid request body', details: result.error.issues });
    }
    const { refreshToken: rawToken } = result.data;

    // Find matching non-revoked token for this user
    const userId = request.user.sub;
    const now = new Date();

    const candidates = await prisma.refreshToken.findMany({
      where: {
        userId,
        revokedAt: null,
        expiresAt: { gt: now },
      },
    });

    let matchedId: string | null = null;
    for (const candidate of candidates) {
      const isMatch = await bcrypt.compare(rawToken, candidate.token);
      if (isMatch) {
        matchedId = candidate.id;
        break;
      }
    }

    if (matchedId) {
      await prisma.refreshToken.update({
        where: { id: matchedId },
        data: { revokedAt: now },
      });
    }

    return reply.code(204).send();
  });
}

export default authRoutes;
export { authRoutes };
