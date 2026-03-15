'use client';
import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import type { License, LicenseType } from '@/types';
import { getAdminLicenses, updateLicense } from '@/lib/api';
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectTrigger, SelectContent, SelectItem, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';

const LICENSE_TYPES: LicenseType[] = ['DAY_PASS', 'MULTI_DAY', 'PERMANENT'];
const LICENSE_LABELS: Record<LicenseType, string> = {
  DAY_PASS: 'Day Pass',
  MULTI_DAY: 'Multi-day Rental',
  PERMANENT: 'Permanent',
};

const schema = z.object({
  type: z.enum(['DAY_PASS', 'MULTI_DAY', 'PERMANENT'] as const),
  expiresAt: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

export default function EditLicensePage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [license, setLicense] = useState<License | null>(null);
  const [loading, setLoading] = useState(true);
  const [revoking, setRevoking] = useState(false);

  const form = useForm<FormValues>({ resolver: zodResolver(schema) });
  const watchedType = form.watch('type');

  useEffect(() => {
    getAdminLicenses()
      .then((all) => {
        const found = all.find((l) => l.id === id);
        if (!found) {
          toast.error('License not found');
          router.push('/licenses');
          return;
        }
        setLicense(found);
        form.reset({
          type: found.type,
          expiresAt: found.expiresAt
            ? new Date(found.expiresAt).toISOString().slice(0, 16)
            : undefined,
        });
        setLoading(false);
      })
      .catch((e: Error) => {
        toast.error(e.message);
        setLoading(false);
      });
  }, [id, form, router]);

  async function onSubmit(values: FormValues) {
    try {
      await updateLicense(id, {
        type: values.type,
        expiresAt: values.expiresAt
          ? new Date(values.expiresAt).toISOString()
          : null,
      });
      toast.success('License updated');
      router.push('/licenses');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Update failed');
    }
  }

  async function handleRevoke() {
    if (!license?.revokedAt) {
      if (!confirm('Revoke this license? The user will immediately lose access.')) return;
    }
    setRevoking(true);
    try {
      const updated = await updateLicense(id, { revoked: !license?.revokedAt });
      setLicense(updated);
      toast.success(updated.revokedAt ? 'License revoked' : 'License reinstated');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Action failed');
    } finally {
      setRevoking(false);
    }
  }

  if (loading) return <p className="text-muted-foreground text-sm">Loading...</p>;
  if (!license) return null;

  const isRevoked = !!license.revokedAt;

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="text-2xl font-semibold">Edit License</h1>

      <div className="text-sm text-muted-foreground space-y-1 border rounded p-3">
        <div><span className="font-medium">User:</span> {license.user.email}</div>
        <div><span className="font-medium">Route:</span> {license.route.title}</div>
        <div className="flex items-center gap-2">
          <span className="font-medium">Status:</span>
          <Badge variant={isRevoked ? 'destructive' : 'default'}>
            {isRevoked ? 'Revoked' : 'Active'}
          </Badge>
        </div>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField control={form.control} name="type" render={({ field }) => (
            <FormItem>
              <FormLabel>License Type *</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl>
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

          <div className="flex gap-3 pt-2 flex-wrap">
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {form.formState.isSubmitting ? 'Saving...' : 'Save Changes'}
            </Button>
            <Button
              type="button"
              variant={isRevoked ? 'outline' : 'destructive'}
              disabled={revoking}
              onClick={handleRevoke}
            >
              {revoking ? 'Working...' : isRevoked ? 'Reinstate License' : 'Revoke License'}
            </Button>
            <Button type="button" variant="outline" onClick={() => router.push('/licenses')}>
              Back
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
