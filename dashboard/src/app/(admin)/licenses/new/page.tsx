'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import type { Route, LicenseType } from '@/types';
import { getAdminRoutes, grantLicense } from '@/lib/api';
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectTrigger, SelectContent, SelectItem, SelectValue } from '@/components/ui/select';

const LICENSE_TYPES: LicenseType[] = ['DAY_PASS', 'MULTI_DAY', 'PERMANENT'];
const LICENSE_LABELS: Record<LicenseType, string> = {
  DAY_PASS: 'Day Pass',
  MULTI_DAY: 'Multi-day Rental',
  PERMANENT: 'Permanent',
};

const schema = z.object({
  email: z.string().email('Invalid email'),
  routeId: z.string().min(1, 'Route required'),
  type: z.enum(['DAY_PASS', 'MULTI_DAY', 'PERMANENT'] as const),
  expiresAt: z.string().optional(),
}).refine((data) => {
  if (data.type !== 'PERMANENT' && !data.expiresAt) {
    return false;
  }
  return true;
}, { message: 'Expiry date required for Day Pass and Multi-day', path: ['expiresAt'] });

type FormValues = z.infer<typeof schema>;

export default function NewLicensePage() {
  const router = useRouter();
  const [routes, setRoutes] = useState<Route[]>([]);

  useEffect(() => {
    getAdminRoutes()
      .then(setRoutes)
      .catch((e: Error) => toast.error(e.message));
  }, []);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: 'PERMANENT' },
  });

  const watchedType = form.watch('type');

  async function onSubmit(values: FormValues) {
    try {
      await grantLicense({
        email: values.email,
        routeId: values.routeId,
        type: values.type,
        expiresAt: values.expiresAt
          ? new Date(values.expiresAt).toISOString()
          : null,
      });
      toast.success('License granted');
      router.push('/licenses');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Grant failed');
    }
  }

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="text-2xl font-semibold">Grant License</h1>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField control={form.control} name="email" render={({ field }) => (
            <FormItem>
              <FormLabel>User Email *</FormLabel>
              <FormControl><Input type="email" placeholder="user@example.com" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="routeId" render={({ field }) => (
            <FormItem>
              <FormLabel>Route *</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Select a route..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {routes.map((r) => (
                    <SelectItem key={r.id} value={r.id}>{r.title}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="type" render={({ field }) => (
            <FormItem>
              <FormLabel>License Type *</FormLabel>
              <Select onValueChange={field.onChange} defaultValue={field.value}>
                <FormControl>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                </FormControl>
                <SelectContent>
                  {LICENSE_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>{LICENSE_LABELS[t]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )} />

          {watchedType !== 'PERMANENT' && (
            <FormField control={form.control} name="expiresAt" render={({ field }) => (
              <FormItem>
                <FormLabel>Expiry Date *</FormLabel>
                <FormControl><Input type="datetime-local" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
          )}

          <div className="flex gap-3 pt-2">
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {form.formState.isSubmitting ? 'Granting...' : 'Grant License'}
            </Button>
            <Button type="button" variant="outline" onClick={() => router.push('/licenses')}>
              Cancel
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
