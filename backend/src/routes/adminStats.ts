import { FastifyInstance } from 'fastify';
import { prisma } from '../lib/prisma';
import { requireAuth } from '../middleware/requireAuth';
import { requireAdmin } from '../middleware/requireAdmin';

async function adminStatsPlugin(fastify: FastifyInstance): Promise<void> {
  fastify.get('/', {
    preHandler: [requireAuth, requireAdmin],
  }, async (_request, reply) => {
    const [
      totalUsers,
      routeStats,
      licenseStats,
      topRoutes,
      recentLicenses,
    ] = await Promise.all([
      // Total users
      prisma.user.count(),

      // Route stats
      prisma.route.groupBy({
        by: ['published'],
        _count: { id: true },
      }),

      // License stats
      prisma.license.findMany({
        select: {
          type: true,
          expiresAt: true,
          revokedAt: true,
          createdAt: true,
        },
      }),

      // Top 8 routes by license count
      prisma.route.findMany({
        where: { published: true },
        select: {
          id: true,
          title: true,
          region: true,
          distanceKm: true,
          viewCount: true,
          navigationCount: true,
          _count: { select: { licenses: true } },
        },
        orderBy: { licenses: { _count: 'desc' } },
        take: 8,
      }),

      // 8 most recent licenses
      prisma.license.findMany({
        select: {
          id: true,
          type: true,
          expiresAt: true,
          revokedAt: true,
          createdAt: true,
          user: { select: { email: true } },
          route: { select: { title: true } },
        },
        orderBy: { createdAt: 'desc' },
        take: 8,
      }),
    ]);

    const now = new Date();
    const activeLicenses   = licenseStats.filter(l => !l.revokedAt && (!l.expiresAt || l.expiresAt > now)).length;
    const expiredLicenses  = licenseStats.filter(l => !l.revokedAt && l.expiresAt && l.expiresAt <= now).length;
    const revokedLicenses  = licenseStats.filter(l => !!l.revokedAt).length;

    const byType = licenseStats.reduce<Record<string, number>>((acc, l) => {
      acc[l.type] = (acc[l.type] ?? 0) + 1;
      return acc;
    }, {});

    const published   = routeStats.find(r => r.published)?._count.id ?? 0;
    const unpublished = routeStats.find(r => !r.published)?._count.id ?? 0;

    return reply.send({
      users: { total: totalUsers },
      routes: { total: published + unpublished, published, unpublished },
      licenses: {
        total: licenseStats.length,
        active: activeLicenses,
        expired: expiredLicenses,
        revoked: revokedLicenses,
        byType,
      },
      topRoutes: topRoutes.map(r => ({
        id: r.id,
        title: r.title,
        region: r.region,
        distanceKm: Number(r.distanceKm),
        viewCount: r.viewCount,
        navigationCount: r.navigationCount,
        licenseCount: r._count.licenses,
      })),
      recentLicenses,
    });
  });
}

export default adminStatsPlugin;
