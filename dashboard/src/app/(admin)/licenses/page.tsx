'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { toast } from 'sonner';
import type { License } from '@/types';
import { getAdminLicenses } from '@/lib/api';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

function getLicenseStatus(license: License): { label: string; variant: 'default' | 'secondary' | 'destructive' } {
  if (license.revokedAt) return { label: 'Revoked', variant: 'destructive' };
  if (license.expiresAt && new Date(license.expiresAt) < new Date()) {
    return { label: 'Expired', variant: 'secondary' };
  }
  return { label: 'Active', variant: 'default' };
}

function formatLicenseType(type: string): string {
  const labels: Record<string, string> = {
    DAY_PASS: 'Day Pass',
    MULTI_DAY: 'Multi-day',
    PERMANENT: 'Permanent',
  };
  return labels[type] ?? type;
}

export default function LicensesPage() {
  const [licenses, setLicenses] = useState<License[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    getAdminLicenses()
      .then(setLicenses)
      .catch((e: Error) => toast.error(e.message))
      .finally(() => setLoading(false));
  }, []);

  const filtered = licenses.filter((l) => {
    const q = filter.toLowerCase();
    return (
      l.user.email.toLowerCase().includes(q) ||
      l.route.title.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Licenses</h1>
        <Button asChild><Link href="/licenses/new">Grant License</Link></Button>
      </div>

      <Input
        placeholder="Filter by email or route name..."
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        className="max-w-sm"
      />

      {loading ? (
        <p className="text-muted-foreground text-sm">Loading...</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>User Email</TableHead>
              <TableHead>Route</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Expires At</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-24">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((license) => {
              const status = getLicenseStatus(license);
              return (
                <TableRow key={license.id}>
                  <TableCell>{license.user.email}</TableCell>
                  <TableCell>{license.route.title}</TableCell>
                  <TableCell>{formatLicenseType(license.type)}</TableCell>
                  <TableCell>
                    {license.expiresAt
                      ? new Date(license.expiresAt).toLocaleDateString()
                      : '—'}
                  </TableCell>
                  <TableCell>
                    <Badge variant={status.variant}>{status.label}</Badge>
                  </TableCell>
                  <TableCell>
                    <Button variant="outline" size="sm" asChild>
                      <Link href={`/licenses/${license.id}`}>Edit</Link>
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
            {filtered.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                  No licenses found.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
