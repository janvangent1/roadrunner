---
phase: 02-admin-dashboard
plan: "02"
subsystem: dashboard-auth
tags: [next.js, shadcn-ui, react-hook-form, zod, jwt, localStorage, middleware]
dependency_graph:
  requires: [02-01]
  provides: [login-page, auth-token-helpers, admin-layout, navbar, route-protection]
  affects: [02-03, 02-04]
tech_stack:
  added: []
  patterns: [client-side-auth-guard, shadcn-ui-copy-paste, localStorage-jwt, react-hook-form-zod]
key_files:
  created:
    - dashboard/src/lib/utils.ts
    - dashboard/src/lib/auth.ts
    - dashboard/src/components/ui/button.tsx
    - dashboard/src/components/ui/input.tsx
    - dashboard/src/components/ui/label.tsx
    - dashboard/src/components/ui/card.tsx
    - dashboard/src/components/ui/form.tsx
    - dashboard/src/app/login/page.tsx
    - dashboard/src/components/NavBar.tsx
    - dashboard/src/app/(admin)/layout.tsx
    - dashboard/src/middleware.ts
  modified: []
decisions:
  - Auth guard is client-side only (useEffect in AdminLayout) because JWT lives in localStorage, not cookies; middleware cannot inspect it
  - Next.js middleware kept minimal but present for future server-side auth if token moves to cookies
metrics:
  duration: "2 min"
  completed_date: "2026-03-14"
  tasks_completed: 2
  files_created: 11
  files_modified: 0
---

# Phase 02 Plan 02: Admin Authentication Summary

**One-liner:** Login page with React Hook Form + Zod validation, localStorage JWT helpers, shared admin layout with NavBar, and Next.js middleware — full client-side route protection for /routes and /licenses.

## Tasks Completed

| # | Task | Commit | Key Files |
|---|------|--------|-----------|
| 1 | Add Shadcn/ui base components and auth utilities | c08b01a | dashboard/src/lib/utils.ts, dashboard/src/lib/auth.ts, dashboard/src/components/ui/{button,input,label,card,form}.tsx |
| 2 | Login page, admin layout with NavBar, and Next.js route protection middleware | 6348b3f | dashboard/src/app/login/page.tsx, dashboard/src/components/NavBar.tsx, dashboard/src/app/(admin)/layout.tsx, dashboard/src/middleware.ts |

## What Was Built

### Auth token helpers (dashboard/src/lib/auth.ts)

Four functions for managing the `accessToken` key in localStorage:
- `saveToken(token)` — writes to localStorage
- `getToken()` — reads from localStorage (SSR-safe: returns null when `window === undefined`)
- `clearToken()` — removes from localStorage
- `isAuthenticated()` — returns `!!getToken()`

### Shadcn/ui components (dashboard/src/components/ui/)

Standard copy-paste Shadcn/ui components:
- `button.tsx` — cva-based Button with default, destructive, outline, secondary, ghost, link variants; sm, default, lg, icon sizes
- `input.tsx` — forwarded ref `<input>` with Tailwind styling
- `label.tsx` — Radix `@radix-ui/react-label` wrapper
- `card.tsx` — Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter
- `form.tsx` — react-hook-form Form primitives: Form (FormProvider), FormField, FormItem, FormLabel, FormControl, FormDescription, FormMessage

### Login page (dashboard/src/app/login/page.tsx)

Client component using React Hook Form + Zod schema (`email`, `password`). On valid submit: calls `api.login()`, stores `accessToken` via `saveToken()`, redirects to `/routes`. Error states shown via `sonner` toast.

### NavBar (dashboard/src/components/NavBar.tsx)

Client component. Shows "Roadrunner Admin" brand, Links to `/routes` and `/licenses` (active link styled `font-medium`), and a Sign out button that calls `clearToken()` and redirects to `/login`.

### Admin layout (dashboard/src/app/(admin)/layout.tsx)

Client component wrapping all protected pages. `useEffect` checks `isAuthenticated()` on mount — redirects to `/login` if false. Renders `<NavBar />` + `<main>` content area.

### Middleware (dashboard/src/middleware.ts)

Next.js edge middleware with PUBLIC_PATHS allowlist (`/login`). Since the JWT is in localStorage (not a cookie), server-side auth inspection is not possible. Auth is enforced client-side in AdminLayout. Middleware exists for future upgrade path if token moves to httpOnly cookie.

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED

Files verified present:
- dashboard/src/lib/utils.ts: FOUND
- dashboard/src/lib/auth.ts: FOUND
- dashboard/src/components/ui/button.tsx: FOUND
- dashboard/src/components/ui/input.tsx: FOUND
- dashboard/src/components/ui/label.tsx: FOUND
- dashboard/src/components/ui/card.tsx: FOUND
- dashboard/src/components/ui/form.tsx: FOUND
- dashboard/src/app/login/page.tsx: FOUND
- dashboard/src/components/NavBar.tsx: FOUND
- dashboard/src/app/(admin)/layout.tsx: FOUND
- dashboard/src/middleware.ts: FOUND

Commits verified:
- c08b01a: FOUND (feat(02-02): add Shadcn/ui components and auth token utilities)
- 6348b3f: FOUND (feat(02-02): add login page, admin layout with NavBar, and route middleware)
