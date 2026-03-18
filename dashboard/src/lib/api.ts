import type { Route, Waypoint, License, LicenseType, WaypointType, AdminStats } from '@/types';

const BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:4000/api/v1';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (res.status === 401) {
    // Token expired or invalid — clear it and redirect to login
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
    }
    throw new Error('Session expired. Please log in again.');
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error((body as { error?: string }).error ?? res.statusText);
  }
  return res.json() as Promise<T>;
}

// Auth
export async function login(email: string, password: string): Promise<{ accessToken: string; refreshToken: string }> {
  return apiFetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
}

// Routes
export async function getAdminRoutes(): Promise<Route[]> {
  return apiFetch('/admin/routes');
}

export async function createRoute(formData: FormData): Promise<Route> {
  return apiFetch('/admin/routes', { method: 'POST', body: formData });
}

export async function updateRoute(id: string, body: Partial<Omit<Route, 'id' | 'createdAt' | 'updatedAt' | 'waypoints'>>): Promise<Route> {
  return apiFetch(`/admin/routes/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export async function deleteRoute(id: string): Promise<void> {
  await apiFetch(`/admin/routes/${id}`, { method: 'DELETE' });
}

export async function replaceWaypoints(routeId: string, waypoints: Omit<Waypoint, 'id'>[]): Promise<Waypoint[]> {
  return apiFetch(`/admin/routes/${routeId}/waypoints`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ waypoints }),
  });
}

// Licenses
export async function getAdminLicenses(filters?: { routeId?: string; userId?: string }): Promise<License[]> {
  const params = new URLSearchParams();
  if (filters?.routeId) params.set('routeId', filters.routeId);
  if (filters?.userId) params.set('userId', filters.userId);
  const qs = params.toString();
  return apiFetch(`/admin/licenses${qs ? `?${qs}` : ''}`);
}

export async function grantLicense(data: { email: string; routeId: string; type: LicenseType; expiresAt: string | null }): Promise<License> {
  return apiFetch('/admin/licenses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export async function updateLicense(id: string, data: { type?: LicenseType; expiresAt?: string | null; revoked?: boolean }): Promise<License> {
  return apiFetch(`/admin/licenses/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export { apiFetch };

// Suppress unused import warning for WaypointType (used by consumers of this module via re-export context)
export type { WaypointType };

export async function getAdminStats(): Promise<AdminStats> {
  return apiFetch('/admin/stats');
}
