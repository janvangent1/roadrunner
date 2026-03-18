'use client';
import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { getAdminStats } from '@/lib/api';
import type { AdminStats } from '@/types';

function StatCard({ label, value, sub, color = 'text-foreground' }: {
  label: string; value: string | number; sub?: string; color?: string;
}) {
  return (
    <div className="rounded-lg border border-border bg-card p-5">
      <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground mb-1">{label}</p>
      <p className={`text-3xl font-bold ${color}`}>{value}</p>
      {sub && <p className="text-xs text-muted-foreground mt-1">{sub}</p>}
    </div>
  );
}

export default function StatsPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAdminStats()
      .then(setStats)
      .catch((e: Error) => toast.error(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="text-muted-foreground text-sm">Loading statistics...</p>;
  if (!stats) return <p className="text-muted-foreground text-sm">Failed to load statistics.</p>;

  const licenseTypeLabels: Record<string, string> = {
    DAY_PASS: 'Day Pass', MULTI_DAY: 'Multi-Day', PERMANENT: 'Permanent',
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold text-foreground">Statistics</h1>
        <p className="text-sm text-muted-foreground mt-0.5">Overview of platform activity</p>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard label="Registered Users" value={stats.users.total} color="text-primary" />
        <StatCard label="Published Routes" value={stats.routes.published}
          sub={`${stats.routes.unpublished} draft`} />
        <StatCard label="Total Licenses" value={stats.licenses.total}
          sub={`${stats.licenses.active} active`} color="text-green-500" />
        <StatCard label="Navigations" value={stats.topRoutes.reduce((s, r) => s + r.navigationCount, 0)}
          sub="total route starts" color="text-primary" />
      </div>

      {/* License breakdown */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div className="rounded-lg border border-border bg-card p-5">
          <h2 className="text-sm font-semibold text-foreground mb-4">License Breakdown</h2>
          <div className="space-y-3">
            {(['DAY_PASS', 'MULTI_DAY', 'PERMANENT'] as const).map(type => {
              const count = stats.licenses.byType[type] ?? 0;
              const pct = stats.licenses.total > 0 ? Math.round((count / stats.licenses.total) * 100) : 0;
              return (
                <div key={type}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-foreground">{licenseTypeLabels[type]}</span>
                    <span className="text-muted-foreground">{count} ({pct}%)</span>
                  </div>
                  <div className="h-2 rounded-full bg-secondary overflow-hidden">
                    <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${pct}%` }} />
                  </div>
                </div>
              );
            })}
            <div className="flex justify-between text-xs text-muted-foreground pt-2 border-t border-border">
              <span>Active: {stats.licenses.active}</span>
              <span>Expired: {stats.licenses.expired}</span>
              <span>Revoked: {stats.licenses.revoked}</span>
            </div>
          </div>
        </div>

        {/* Recent licenses */}
        <div className="rounded-lg border border-border bg-card p-5">
          <h2 className="text-sm font-semibold text-foreground mb-4">Recent Licenses</h2>
          <div className="space-y-3">
            {stats.recentLicenses.map(l => (
              <div key={l.id} className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-sm text-foreground truncate">{l.user.email}</p>
                  <p className="text-xs text-muted-foreground truncate">{l.route.title}</p>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-xs font-medium text-foreground">{licenseTypeLabels[l.type] ?? l.type}</p>
                  <p className="text-xs text-muted-foreground">
                    {new Date(l.createdAt).toLocaleDateString()}
                  </p>
                </div>
              </div>
            ))}
            {stats.recentLicenses.length === 0 && (
              <p className="text-sm text-muted-foreground">No licenses yet.</p>
            )}
          </div>
        </div>
      </div>

      {/* Top routes table */}
      <div className="rounded-lg border border-border overflow-hidden">
        <div className="bg-secondary/50 px-5 py-3 border-b border-border">
          <h2 className="text-sm font-semibold text-foreground">Route Performance</h2>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left">
              <th className="px-5 py-3 font-medium text-muted-foreground">Route</th>
              <th className="px-4 py-3 font-medium text-muted-foreground text-right">Views</th>
              <th className="px-4 py-3 font-medium text-muted-foreground text-right">Navigations</th>
              <th className="px-4 py-3 font-medium text-muted-foreground text-right">Licenses</th>
              <th className="px-4 py-3 font-medium text-muted-foreground text-right">Distance</th>
            </tr>
          </thead>
          <tbody>
            {stats.topRoutes.map((route, i) => (
              <tr key={route.id} className={`border-b border-border hover:bg-secondary/50 transition-colors ${i % 2 === 0 ? '' : 'bg-secondary/20'}`}>
                <td className="px-5 py-3">
                  <p className="font-medium text-foreground">{route.title}</p>
                  <p className="text-xs text-muted-foreground">{route.region}</p>
                </td>
                <td className="px-4 py-3 text-right text-muted-foreground">{route.viewCount}</td>
                <td className="px-4 py-3 text-right text-muted-foreground">{route.navigationCount}</td>
                <td className="px-4 py-3 text-right">
                  <span className="font-semibold text-primary">{route.licenseCount}</span>
                </td>
                <td className="px-4 py-3 text-right text-muted-foreground">{route.distanceKm} km</td>
              </tr>
            ))}
            {stats.topRoutes.length === 0 && (
              <tr><td colSpan={5} className="px-5 py-10 text-center text-muted-foreground">No routes yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
