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
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Routes</h1>
          <p className="text-sm text-muted-foreground mt-0.5">Manage and publish motorcycle routes</p>
        </div>
        <Button asChild className="bg-primary hover:bg-primary/90 text-white font-medium">
          <Link href="/routes/new">Upload route</Link>
        </Button>
      </div>

      {loading ? (
        <p className="text-muted-foreground text-sm">Loading...</p>
      ) : (
        <div className="rounded-lg border border-border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="border-border bg-secondary/50 hover:bg-secondary/50">
                <TableHead className="text-muted-foreground font-medium">Title</TableHead>
                <TableHead className="text-muted-foreground font-medium">Region</TableHead>
                <TableHead className="text-muted-foreground font-medium">Difficulty</TableHead>
                <TableHead className="text-muted-foreground font-medium">Distance</TableHead>
                <TableHead className="text-muted-foreground font-medium">Status</TableHead>
                <TableHead className="w-40 text-muted-foreground font-medium">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {routes.map((route) => (
                <TableRow key={route.id} className="border-border hover:bg-secondary/50 transition-colors">
                  <TableCell className="font-medium text-foreground">{route.title}</TableCell>
                  <TableCell className="text-muted-foreground">{route.region}</TableCell>
                  <TableCell className="text-muted-foreground">{route.difficulty}</TableCell>
                  <TableCell className="text-muted-foreground">{route.distanceKm} km</TableCell>
                  <TableCell>
                    {route.published ? (
                      <Badge className="bg-success/15 text-success border-success/30 hover:bg-success/15">
                        Published
                      </Badge>
                    ) : (
                      <Badge variant="secondary" className="text-muted-foreground">
                        Draft
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" asChild
                        className="border-border text-foreground hover:bg-secondary hover:text-foreground">
                        <Link href={`/routes/${route.id}`}>Edit</Link>
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => togglePublished(route)}
                        className="border-border text-foreground hover:bg-secondary hover:text-foreground"
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
              {routes.length === 0 && !loading && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-12">
                    No routes yet. Upload your first route.
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
