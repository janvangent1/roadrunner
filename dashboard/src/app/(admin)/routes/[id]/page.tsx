'use client';
import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import type { Route, Waypoint } from '@/types';
import { updateRoute, deleteRoute, replaceWaypoints, apiFetch } from '@/lib/api';
import type { WaypointRow } from '@/components/WaypointEditor';
import { WaypointEditor } from '@/components/WaypointEditor';
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectTrigger, SelectContent, SelectItem, SelectValue } from '@/components/ui/select';

const DIFFICULTIES = ['EASY', 'MODERATE', 'HARD', 'EXTREME'] as const;

const schema = z.object({
  title: z.string().min(1, 'Title required'),
  description: z.string().optional(),
  difficulty: z.enum(DIFFICULTIES),
  terrainType: z.string().min(1, 'Required'),
  region: z.string().min(1, 'Required'),
  estimatedDurationMinutes: z.coerce.number().int().positive(),
  published: z.boolean(),
});
type FormValues = z.infer<typeof schema>;

export default function EditRoutePage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [loading, setLoading] = useState(true);
  const [waypoints, setWaypoints] = useState<WaypointRow[]>([]);
  const [savingWaypoints, setSavingWaypoints] = useState(false);
  const [computedDistance, setComputedDistance] = useState<number | null>(null);

  const form = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    apiFetch<Route>(`/routes/${id}`)
      .then((route) => {
        form.reset({
          title: route.title,
          description: route.description ?? '',
          difficulty: route.difficulty,
          terrainType: route.terrainType,
          region: route.region,
          estimatedDurationMinutes: route.estimatedDurationMinutes,
          published: route.published,
        });
        setComputedDistance(route.distanceKm);
        if (route.waypoints) {
          setWaypoints(
            route.waypoints.map((w: Waypoint) => ({
              label: w.label,
              latitude: String(w.latitude),
              longitude: String(w.longitude),
              type: w.type,
            }))
          );
        }
        setLoading(false);
      })
      .catch((e: Error) => {
        toast.error(e.message);
        setLoading(false);
      });
  }, [id, form]);

  async function onSubmitMetadata(values: FormValues) {
    try {
      await updateRoute(id, {
        title: values.title,
        description: values.description ?? null,
        difficulty: values.difficulty,
        terrainType: values.terrainType,
        region: values.region,
        estimatedDurationMinutes: values.estimatedDurationMinutes,
        published: values.published,
      });
      toast.success('Route updated');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Update failed');
    }
  }

  async function saveWaypoints() {
    setSavingWaypoints(true);
    try {
      const waypointData = waypoints.map((wp, i) => ({
        label: wp.label,
        latitude: parseFloat(wp.latitude),
        longitude: parseFloat(wp.longitude),
        type: wp.type,
        sortOrder: i,
      }));
      await replaceWaypoints(id, waypointData);
      toast.success('Waypoints saved');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Save failed');
    } finally {
      setSavingWaypoints(false);
    }
  }

  async function handleDelete() {
    if (!confirm('Delete this route? This cannot be undone.')) return;
    try {
      await deleteRoute(id);
      toast.success('Route deleted');
      router.push('/routes');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Delete failed');
    }
  }

  if (loading) return <p className="text-muted-foreground text-sm">Loading...</p>;

  return (
    <div className="max-w-2xl space-y-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Edit Route</h1>
        <Button variant="destructive" size="sm" onClick={handleDelete}>Delete Route</Button>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmitMetadata)} className="space-y-4">
          <FormField control={form.control} name="title" render={({ field }) => (
            <FormItem><FormLabel>Title *</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
          )} />
          <FormField control={form.control} name="description" render={({ field }) => (
            <FormItem><FormLabel>Description</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
          )} />
          <div className="grid grid-cols-2 gap-4">
            <FormField control={form.control} name="difficulty" render={({ field }) => (
              <FormItem>
                <FormLabel>Difficulty *</FormLabel>
                <Select onValueChange={field.onChange} value={field.value}>
                  <FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl>
                  <SelectContent>{DIFFICULTIES.map((d) => <SelectItem key={d} value={d}>{d}</SelectItem>)}</SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )} />
            <FormField control={form.control} name="terrainType" render={({ field }) => (
              <FormItem><FormLabel>Terrain Type *</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
            )} />
            <FormField control={form.control} name="region" render={({ field }) => (
              <FormItem><FormLabel>Region *</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
            )} />
            <FormField control={form.control} name="estimatedDurationMinutes" render={({ field }) => (
              <FormItem><FormLabel>Duration (min) *</FormLabel><FormControl><Input type="number" {...field} /></FormControl><FormMessage /></FormItem>
            )} />
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground/80">Distance (km)</label>
              <div className="flex h-9 items-center rounded-md border border-border bg-secondary/30 px-3 text-sm text-muted-foreground">
                {computedDistance != null ? `${computedDistance} km (auto-computed)` : 'Auto-computed from GPX'}
              </div>
            </div>
          </div>

          <FormField control={form.control} name="published" render={({ field }) => (
            <FormItem className="flex items-center gap-3 space-y-0">
              <FormControl>
                <input
                  type="checkbox"
                  checked={field.value}
                  onChange={field.onChange}
                  className="h-4 w-4"
                />
              </FormControl>
              <FormLabel className="font-normal">Published (visible in app catalog)</FormLabel>
            </FormItem>
          )} />

          <div className="flex gap-3">
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {form.formState.isSubmitting ? 'Saving...' : 'Save Metadata'}
            </Button>
            <Button type="button" variant="outline" onClick={() => router.push('/routes')}>
              Back
            </Button>
          </div>
        </form>
      </Form>

      <div className="border-t pt-6 space-y-4">
        <h2 className="text-lg font-medium">Waypoints / POIs</h2>
        <WaypointEditor value={waypoints} onChange={setWaypoints} />
        <Button type="button" onClick={saveWaypoints} disabled={savingWaypoints} variant="outline">
          {savingWaypoints ? 'Saving...' : 'Save Waypoints'}
        </Button>
      </div>
    </div>
  );
}
