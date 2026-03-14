import { buildApp } from './app';
import { initTink } from './lib/tink';

async function start(): Promise<void> {
  // Initialize Tink before starting the server — required for GPX encryption
  await initTink();

  const server = await buildApp();

  const port = parseInt(process.env.PORT || '3000', 10);
  const host = '0.0.0.0';

  try {
    await server.listen({ port, host });
    server.log.info(`Server listening on ${host}:${port}`);
  } catch (err) {
    server.log.error(err);
    process.exit(1);
  }

  const shutdown = async (): Promise<void> => {
    server.log.info('Received shutdown signal, closing server...');
    try {
      await server.close();
      server.log.info('Server closed gracefully');
      process.exit(0);
    } catch (err) {
      server.log.error({ err }, 'Error during shutdown');
      process.exit(1);
    }
  };

  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

start();
