import Redis from 'ioredis';

const REDIS_URL = process.env.REDIS_URL || 'redis://localhost:6379';

const globalForRedis = globalThis as unknown as { redis: Redis };

export const redis = globalForRedis.redis ?? new Redis(REDIS_URL, {
  lazyConnect: true,
  maxRetriesPerRequest: 3,
});

if (process.env.NODE_ENV !== 'production') globalForRedis.redis = redis;

redis.on('error', (err) => {
  console.error('[Redis] Connection error:', err.message);
});

redis.on('connect', () => {
  console.log('[Redis] Connected');
});

/**
 * Get a value from Redis. Returns null if key does not exist or on error.
 */
export async function redisGet(key: string): Promise<string | null> {
  try {
    return await redis.get(key);
  } catch (err) {
    console.error('[Redis] GET error for key', key, err);
    return null;
  }
}

/**
 * Set a value in Redis with a TTL in seconds.
 */
export async function redisSet(key: string, value: string, ttlSeconds: number): Promise<void> {
  try {
    await redis.set(key, value, 'EX', ttlSeconds);
  } catch (err) {
    console.error('[Redis] SET error for key', key, err);
  }
}
