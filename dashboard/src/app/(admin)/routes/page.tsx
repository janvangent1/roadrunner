'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { toast } from 'sonner';
import type { Route } from '@/types';
import { getAdminRoutes, updateRoute, deleteRoute } from '@/lib/api';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

export default function RoutesPage() {
  const [routes, setRoutes] = useState<Route[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAdminRoutes()
      .then(setRoutes)
      .catch((e: Error) => toast.error(e.message))
      .finally(() => setLoading(false));
  }, []);

  async function togglePublished(route: Route) {
    try {
      const updated = await updateRoute(route.id, { published: !route.published });
      setRoutes((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
      toast.success(updated.published ? 'Route published' : 'Route unpublished');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Update failed');
    }
  }

  async function handleDelete(route: Route) {
    if (!confirm(`Delete "${route.title}"? This cannot be undone.`)) return;
    try {
      await deleteRoute(route.id);
      setRoutes((prev) => prev.filter((r) => r.id !== route.id));
      toast.success('Route deleted');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Delete failed');
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Routes</h1>
        <Button asChild>
          <Link href="/routes/new">Upload route</Link>
        </Button>
      </div>

      {loading ? (
        <p className="text-muted-foreground text-sm">Loading...</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Title</TableHead>
              <TableHead>Region</TableHead>
              <TableHead>Difficulty</TableHead>
              <TableHead>Distance</TableHead>
              <TableHead>Published</TableHead>
              <TableHead className="w-40">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {routes.map((route) => (
              <TableRow key={route.id}>
                <TableCell className="font-medium">{route.title}</TableCell>
                <TableCell>{route.region}</TableCell>
                <TableCell>{route.difficulty}</TableCell>
                <TableCell>{route.distanceKm} km</TableCell>
                <TableCell>
                  <Badge variant={route.published ? 'default' : 'secondary'}>
                    {route.published ? 'Published' : 'Draft'}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" asChild>
                      <Link href={`/routes/${route.id}`}>Edit</Link>
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => togglePublished(route)}
                    >
                      {route.published ? 'Unpublish' : 'Publish'}
                    </Button>
                    <Button
                      variant="destructive"
                      size="sm"
                      onClick={() => handleDelete(route)}
                    >
                      Delete
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
