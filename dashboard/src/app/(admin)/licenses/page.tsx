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
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Licenses</h1>
          <p className="text-sm text-muted-foreground mt-0.5">Manage user route access licenses</p>
        </div>
        <Button asChild className="bg-primary hover:bg-primary/90 text-white font-medium">
          <Link href="/licenses/new">Grant license</Link>
        </Button>
      </div>

      <Input
        placeholder="Filter by email or route name..."
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        className="max-w-sm bg-secondary border-border text-foreground placeholder:text-muted-foreground"
      />

      {loading ? (
        <p className="text-muted-foreground text-sm">Loading...</p>
      ) : (
        <div className="rounded-lg border border-border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="border-border bg-secondary/50 hover:bg-secondary/50">
                <TableHead className="text-muted-foreground font-medium">User Email</TableHead>
                <TableHead className="text-muted-foreground font-medium">Route</TableHead>
                <TableHead className="text-muted-foreground font-medium">Type</TableHead>
                <TableHead className="text-muted-foreground font-medium">Expires At</TableHead>
                <TableHead className="text-muted-foreground font-medium">Status</TableHead>
                <TableHead className="w-24 text-muted-foreground font-medium">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((license) => {
                const status = getLicenseStatus(license);
                return (
                  <TableRow key={license.id} className="border-border hover:bg-secondary/50 transition-colors">
                    <TableCell className="text-foreground">{license.user.email}</TableCell>
                    <TableCell className="text-muted-foreground">{license.route.title}</TableCell>
                    <TableCell className="text-muted-foreground">{formatLicenseType(license.type)}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {license.expiresAt
                        ? new Date(license.expiresAt).toLocaleDateString()
                        : '\u2014'}
                    </TableCell>
                    <TableCell>
                      {status.variant === 'default' && (
                        <Badge className="bg-success/15 text-success border-success/30 hover:bg-success/15">
                          {status.label}
                        </Badge>
                      )}
                      {status.variant === 'destructive' && (
                        <Badge className="bg-destructive/15 text-destructive border-destructive/30 hover:bg-destructive/15">
                          {status.label}
                        </Badge>
                      )}
                      {status.variant === 'secondary' && (
                        <Badge variant="secondary" className="text-muted-foreground">
                          {status.label}
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell>
                      <Button variant="outline" size="sm" asChild
                        className="border-border text-foreground hover:bg-secondary hover:text-foreground">
                        <Link href={`/licenses/${license.id}`}>Edit</Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
              {filtered.length === 0 && !loading && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-12">
                    No licenses found.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
