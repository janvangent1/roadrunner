'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { createRoute } from '@/lib/api';
import type { WaypointRow } from '@/components/WaypointEditor';
import { WaypointEditor } from '@/components/WaypointEditor';
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectTrigger,
  SelectContent,
  SelectItem,
  SelectValue,
} from '@/components/ui/select';

const DIFFICULTIES = ['EASY', 'MODERATE', 'HARD', 'EXTREME'] as const;

const schema = z.object({
  title: z.string().min(1, 'Title required'),
  description: z.string().optional(),
  difficulty: z.enum(DIFFICULTIES),
  terrainType: z.string().min(1, 'Terrain type required'),
  region: z.string().min(1, 'Region required'),
  estimatedDurationMinutes: z.coerce.number().int().positive('Must be positive'),
});
type FormValues = z.infer<typeof schema>;

export default function NewRoutePage() {
  const router = useRouter();
  const [gpxFile, setGpxFile] = useState<File | null>(null);
  const [waypoints, setWaypoints] = useState<WaypointRow[]>([]);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { difficulty: 'MODERATE' },
  });

  async function onSubmit(values: FormValues) {
    if (!gpxFile) {
      toast.error('Please select a GPX file');
      return;
    }

    // Validate waypoints
    for (const wp of waypoints) {
      if (!wp.label || !wp.latitude || !wp.longitude) {
        toast.error('All waypoint fields are required');
        return;
      }
    }

    const formData = new FormData();
    formData.append('gpx', gpxFile);
    formData.append('title', values.title);
    if (values.description) formData.append('description', values.description);
    formData.append('difficulty', values.difficulty);
    formData.append('terrainType', values.terrainType);
    formData.append('region', values.region);
    formData.append('estimatedDurationMinutes', String(values.estimatedDurationMinutes));

    if (waypoints.length > 0) {
      const waypointsData = waypoints.map((wp, i) => ({
        label: wp.label,
        latitude: parseFloat(wp.latitude),
        longitude: parseFloat(wp.longitude),
        type: wp.type,
        sortOrder: i,
      }));
      formData.append('waypoints', JSON.stringify(waypointsData));
    }

    try {
      await createRoute(formData);
      toast.success('Route created');
      router.push('/routes');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Create failed');
    }
  }

  return (
    <div className="max-w-2xl space-y-6">
      <h1 className="text-2xl font-semibold">Upload Route</h1>

      <div className="space-y-2">
        <label className="text-sm font-medium">GPX File *</label>
        <Input
          type="file"
          accept=".gpx,application/gpx+xml,application/xml,text/xml"
          onChange={(e) => setGpxFile(e.target.files?.[0] ?? null)}
        />
        <p className="text-xs text-muted-foreground">
          Distance will be automatically calculated from the GPX file.
        </p>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField
            control={form.control}
            name="title"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Title *</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="description"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Description</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="space-y-4">
            <FormField
              control={form.control}
              name="difficulty"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Difficulty *</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {DIFFICULTIES.map((d) => (
                        <SelectItem key={d} value={d}>
                          {d}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="terrainType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Terrain Type *</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g. Gravel" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="region"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Region *</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g. NSW Blue Mountains" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="estimatedDurationMinutes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Duration (minutes) *</FormLabel>
                  <FormControl>
                    <Input type="number" min="1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <WaypointEditor value={waypoints} onChange={setWaypoints} />

          <div className="flex gap-3 pt-2">
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {form.formState.isSubmitting ? 'Uploading...' : 'Upload Route'}
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push('/routes')}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
