export type Difficulty = 'EASY' | 'MODERATE' | 'HARD' | 'EXTREME';
export type WaypointType = 'FUEL' | 'WATER' | 'CAUTION' | 'INFO';
export type LicenseType = 'DAY_PASS' | 'MULTI_DAY' | 'PERMANENT';

export interface Waypoint {
  id: string;
  label: string;
  latitude: number;
  longitude: number;
  type: WaypointType;
  sortOrder: number;
}

export interface Route {
  id: string;
  title: string;
  description: string | null;
  difficulty: Difficulty;
  terrainType: string;
  region: string;
  estimatedDurationMinutes: number;
  distanceKm: number;
  published: boolean;
  createdAt: string;
  updatedAt: string;
  waypoints?: Waypoint[];
  priceDayPass?: number | null;
  priceMultiDay?: number | null;
  pricePermanent?: number | null;
  viewCount?: number;
  navigationCount?: number;
}

export interface License {
  id: string;
  userId: string;
  routeId: string;
  type: LicenseType;
  expiresAt: string | null;
  revokedAt: string | null;
  createdAt: string;
  user: { id: string; email: string };
  route: { id: string; title: string };
}

export interface ApiError {
  error: string;
  details?: unknown;
}

export interface AdminStats {
  users: { total: number };
  routes: { total: number; published: number; unpublished: number };
  licenses: {
    total: number;
    active: number;
    expired: number;
    revoked: number;
    byType: Record<string, number>;
  };
  topRoutes: Array<{
    id: string;
    title: string;
    region: string;
    distanceKm: number;
    viewCount: number;
    navigationCount: number;
    licenseCount: number;
  }>;
  recentLicenses: Array<{
    id: string;
    type: string;
    expiresAt: string | null;
    revokedAt: string | null;
    createdAt: string;
    user: { email: string };
    route: { title: string };
  }>;
}
