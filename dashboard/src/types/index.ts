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
