import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { google } from 'googleapis';

const verifyBodySchema = z.object({
  token: z.string(),
  nonce: z.string(),
});

async function integrityHandlers(fastify: FastifyInstance) {
  // POST /api/v1/integrity/verify
  // Decodes a Play Integrity token server-side and returns { passed: boolean }
  fastify.post('/verify', async (request, reply) => {
    const result = verifyBodySchema.safeParse(request.body);
    if (!result.success) {
      return reply.status(400).send({ error: 'Invalid request body' });
    }

    const { token } = result.data;

    try {
      const auth = new google.auth.GoogleAuth({
        scopes: ['https://www.googleapis.com/auth/playintegrity'],
      });

      const playIntegrity = google.playintegrity({ version: 'v1', auth });
      const packageName = process.env.ANDROID_PACKAGE_NAME || 'com.roadrunner.app';

      const response = await playIntegrity.v1.decodeIntegrityToken({
        packageName,
        requestBody: { integrityToken: token },
      });

      const verdict = response.data.tokenPayloadExternal;
      const deviceIntegrity = verdict?.deviceIntegrity?.deviceRecognitionVerdict ?? [];

      // MEETS_BASIC_INTEGRITY must be present for device to pass
      const passed = deviceIntegrity.includes('MEETS_BASIC_INTEGRITY');

      return reply.status(200).send({ passed });
    } catch (err) {
      fastify.log.error({ err }, 'Play Integrity API call failed');
      // On API error, return passed=false (fail-secure)
      return reply.status(200).send({ passed: false, reason: 'integrity_api_error' });
    }
  });
}

export default integrityHandlers;
