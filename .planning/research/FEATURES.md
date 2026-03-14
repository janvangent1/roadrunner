# Feature Research

**Domain:** Android GPX route navigation app with licensed/premium route content (offroad motorcycle)
**Researched:** 2026-03-14
**Confidence:** MEDIUM-HIGH (competitors researched via web; no app-internal testing performed)

---

## Feature Landscape

### Table Stakes (Users Expect These)

Features that any route navigation app with paid content must have. Missing one of these will result in user refunds or negative reviews, regardless of how good the rest of the product is.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Route catalog with list view | Users need to browse what they can buy before committing | LOW | Title, thumbnail map, distance, difficulty minimum. AllTrails, Komoot, onX all open with a catalog. |
| Route detail page before purchase | Riders need to assess fit before paying | MEDIUM | Distance, elevation profile, difficulty rating, terrain type (gravel/dirt/paved mix), estimated duration, region/area name, preview map with route line visible but data locked |
| Difficulty rating per route | Riders must pre-screen for their skill level | LOW | Standard scale (e.g. Easy / Moderate / Hard / Expert). onX uses color-coded ratings. Absence means safety risk. |
| In-app purchase via Google Play Billing | Users expect Play Store checkout — not a web redirect | HIGH | Day pass, multi-day rental, and permanent purchase are all one-time products. Rental type natively supported in Play Billing (rental period + optional expiration period). |
| Post-purchase route navigation (map + route overlay) | The core product experience — without this nothing works | HIGH | OSMDroid base map, encrypted GPX rendered as polyline overlay, current GPS position dot. |
| Offline map tiles for purchased routes | Offroad means no cell signal. Navigation must work offline. | HIGH | OSMDroid supports tile pre-caching. This is not optional for the target use case — it is a safety requirement. |
| Encrypted offline route storage | Users will not tolerate "you need signal to ride" | HIGH | GPX decrypted only at render time; never written to accessible storage. Core business constraint, also a user experience requirement (remote terrain). |
| License status visible on route | Users need to know what they own, what's expired, what's available | LOW | Badge or label: "Owned", "Expires in 2 days", "Rental expired", "Day pass active". |
| License enforcement with expiry communication | Riders must know before they go, not mid-ride | MEDIUM | Show expiry time on route detail and during active navigation. Block navigation on expired license. PROJECT.md specifies 1-hour grace period for active sessions — this is table stakes for trust. |
| User account (login / sign-up) | License is account-bound — account is mandatory | MEDIUM | Email + password minimum. Google Sign-In strongly preferred on Android (one tap, no friction). Account ties purchases to identity for server-side validation. |
| Purchased routes accessible from "My Routes" / library | Users expect to find what they bought | LOW | Separate from the browse catalog. Filtered to owned/licensed routes only. |
| Basic route metadata | Distance, duration, surface type, region | LOW | Without this a route listing is just a name. All comparable apps (onX, AllTrails, REVER) show this immediately. |

---

### Differentiators (Competitive Advantage)

Features that go beyond what users assume. These are where Roadrunner can win specifically against the generic navigation app market, given the curated + licensed model.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Curated route quality (not user-generated) | Offroad riders distrust random GPX quality — admin-curated means trusted, vetted, correct | LOW (process) / MEDIUM (tooling) | The differentiator is the content model, not a feature per se. The web dashboard enables it. Competitors (Wikiloc, onX community trails) are crowdsourced; this is expert-curated. |
| Elevation profile in route preview | Riders assess physical demand before committing to a day pass purchase | MEDIUM | OSMDroid does not provide elevation natively — requires external elevation API (e.g. Open-Elevation or pre-computed from GPX Z-values) or SRTM data. If GPX has Z-values, this is LOW complexity. |
| Active navigation ride stats HUD | Speed, distance covered, distance remaining, elapsed time during a ride | MEDIUM | Competitors (REVER, onX) offer this. For motorcycle use, having glanceable stats without leaving the map is high value. Single-screen HUD overlay on top of map. |
| License type selector on purchase screen | Rider can choose Day Pass / Multi-day / Permanent at time of purchase | MEDIUM | Most apps offer one price. Offering time-limited access lowers barrier to first purchase (try before committing). Unique to this model. |
| "Ride again" / rental renewal prompt at expiry | Proactive renewal CTA when license nears end | LOW | Push notification or in-app banner. Converts casual renters to permanent purchasers. No competitor in this niche does this well. |
| Route region / area grouping | Riders plan by geography (e.g. "what routes are near Andalusia?") | LOW | Map-based browsing or region filter on catalog. onX does this well; most hiking apps use it. Given the catalog is small (single seller), this is achievable early. |
| Offline map pre-download prompt on purchase | Immediately after purchase, prompt user to download tiles for that route | LOW | Reduces "no map when I get there" support requests. Simple UX improvement with high real-world impact for offroad riders. |
| Waypoints / POIs on route | Mark key points: tricky intersections, fuel stops, water crossings | MEDIUM | Admin adds these during GPX upload. Displayed as map pins during navigation. onX and Ride with GPS both use waypoints as a premium differentiator. |

---

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| GPX file export / download | "I want a backup" / "I want to use it in my Garmin" | Destroys the core business model — the licensed data protection is the product. Once exported, license enforcement is unenforceable. | Redirect to in-app navigation. If Garmin support is needed later, investigate encrypted Garmin file transfer — do not expose raw GPX. |
| User-uploaded / community routes | "I made a great route, let me share it" | Introduces content moderation burden, quality dilution, and liability for incorrect trail data. The curated model is the value proposition. | Admin-only uploads via web dashboard. Collect user route suggestions as feedback, not as uploads. |
| Social features (likes, comments, reviews) | Community engagement feels valuable | Scope explosion for v1. Moderation overhead. Competing with Wikiloc/AllTrails on their home turf, where Roadrunner has no advantage. | Star ratings on purchased routes (simple, no moderation needed) as a v1.x feature. |
| Turn-by-turn voice guidance | "Other apps have this" | Explicitly out of scope per PROJECT.md. High complexity, TTS/audio routing conflicts with motorcycle intercom systems. | Visual turn indicators are sufficient for offroad motorcycle navigation at lower speeds. Add voice in v2 if validated. |
| iOS version | "What about iPhone users?" | Cross-platform doubles engineering and testing overhead. Android-only is the right v1 constraint. | Defer explicitly. Architecture should allow future cross-platform port (e.g. via KMM or React Native rewrite), but v1 is Android-only. |
| Real-time trail conditions / weather | "Tell me if the trail is muddy" | Requires data partnerships or crowdsourced reports — neither exists at launch. Creates expectation the admin cannot fulfill. | Document trail season/best-period in route description metadata instead. |
| Offline routing / rerouting | "Recalculate if I go off-route" | OSMDroid does not include a routing engine. Adding one (e.g. GraphHopper embedded) is significant complexity. Offroad routes often deviate intentionally. | Show "off route" indicator visually (GPS dot diverges from route line). Do not attempt rerouting in v1. |
| Multiple sellers / marketplace | "Let other guides sell their routes" | Doubles backend complexity (seller accounts, revenue split, content moderation, tax implications). Single-seller model is a deliberate constraint. | Explicit non-goal for v1. If validated, evaluate as a separate architectural phase. |

---

## Feature Dependencies

```
[User Account / Authentication]
    └──required by──> [Purchase via Google Play Billing]
                          └──required by──> [License Enforcement (server-side)]
                                                └──required by──> [Navigation unlock/block]

[Offline Map Tile Download]
    └──required by──> [Offline Navigation]

[GPX Upload (admin dashboard)]
    └──required by──> [Route Catalog]
                          └──required by──> [Route Detail Page]
                                                └──required by──> [Purchase Flow]

[Route purchased / license active]
    └──required by──> [Navigation launch]
    └──required by──> [Offline map pre-download prompt]

[Elevation data in GPX (Z-values)]
    └──enables──> [Elevation profile display] (LOW complexity if present, MEDIUM if external API needed)

[Waypoints in GPX]
    └──enhances──> [Navigation HUD / map display]

[Route Detail Page] ──enhances──> [Purchase conversion]
[Offline map pre-download prompt] ──enhances──> [Offline Navigation reliability]
[License expiry banner] ──enhances──> [Rental renewal / upsell]

[GPX export] ──conflicts with──> [Data protection / encrypted storage] (deliberate anti-feature)
[Community uploads] ──conflicts with──> [Curated catalog quality model]
```

### Dependency Notes

- **User Account required before Purchase:** Google Play Billing purchase must be linkable to a server-side identity for license enforcement. Without an account, license validation is impossible.
- **GPX upload required before Route Catalog:** The catalog is empty without admin-uploaded routes. The web dashboard (admin tooling) must ship before or alongside the app.
- **Offline tile download required for Offline Navigation:** OSMDroid can use cached tiles, but tiles must be explicitly pre-downloaded for the route area. This is a user-triggered action but should be prompted at purchase time.
- **License active check gates Navigation launch:** Every navigation start must verify license status. The 1-hour grace period for active sessions is a UX safety net, not a bypass — server must log session start time.
- **Elevation profile is an enhancement, not a blocker:** If GPX files contain Z-values (altitude), this is cheap to render. If not, an external elevation API adds a server dependency and latency.

---

## MVP Definition

### Launch With (v1)

Minimum viable product to validate that riders will pay for curated, licensed, encrypted GPX routes.

- [ ] User account (email/password + Google Sign-In) — license binding requires identity
- [ ] Route catalog: list with title, distance, difficulty, region, thumbnail map — browsing is the entry point
- [ ] Route detail: distance, difficulty, terrain type, preview map (route line visible, data locked), purchase options — decision-enabling info before payment
- [ ] Google Play Billing purchase flow: day pass, multi-day rental, permanent — the business model
- [ ] License status display on route detail and catalog — trust and transparency
- [ ] Navigation screen: OSMDroid base map + encrypted GPX polyline overlay + GPS position dot — core product
- [ ] Offline map tile pre-caching for purchased route area — offroad safety requirement
- [ ] Encrypted GPX storage, decrypt-at-render only — core business protection
- [ ] License enforcement: block navigation on expired license, 1-hour grace period for active sessions — integrity
- [ ] Server-side license validation — prevent client-side bypass
- [ ] My Routes / library screen: list of purchased routes — basic ownership UX
- [ ] Admin web dashboard: GPX upload, route metadata entry, catalog management — without this there are no routes

### Add After Validation (v1.x)

Add once v1 is live and user behavior is observable.

- [ ] Elevation profile in route detail — add when GPX data quality is confirmed and if users request it
- [ ] Ride stats HUD during navigation (speed, distance, elapsed time) — add when navigation UX is validated
- [ ] Waypoints / POIs on route — add when admin has enough routes to benefit from point-of-interest annotation
- [ ] License renewal / expiry push notification — add when first rentals start expiring (monitor churn)
- [ ] Route region/area browsing (map-based or filter) — add when catalog has 10+ routes across multiple areas
- [ ] Offline map pre-download prompt immediately post-purchase — quick UX win, low effort

### Future Consideration (v2+)

Defer until product-market fit is established and sport car variant is scoped.

- [ ] Turn-by-turn voice guidance — high complexity, out of scope per PROJECT.md; validate demand first
- [ ] Social / ratings on routes — requires moderation strategy; validate if community is desired
- [ ] Multi-seller / marketplace model — architectural change; only if single-seller model saturates
- [ ] iOS version — separate project; defer until Android v1 is profitable
- [ ] Sport car app variant — separate app per PROJECT.md; re-skin from same architecture

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| User account / auth | HIGH | MEDIUM | P1 |
| Route catalog | HIGH | LOW | P1 |
| Route detail page | HIGH | LOW | P1 |
| Google Play Billing purchase flow | HIGH | HIGH | P1 |
| Navigation screen (map + route overlay) | HIGH | HIGH | P1 |
| Encrypted GPX storage | HIGH | HIGH | P1 |
| Offline map tile pre-caching | HIGH | MEDIUM | P1 |
| License enforcement + grace period | HIGH | HIGH | P1 |
| Server-side license validation | HIGH | HIGH | P1 |
| My Routes / library screen | MEDIUM | LOW | P1 |
| Admin web dashboard (GPX upload) | HIGH | HIGH | P1 |
| License status display | MEDIUM | LOW | P1 |
| Elevation profile | MEDIUM | LOW-MEDIUM | P2 |
| Ride stats HUD | MEDIUM | MEDIUM | P2 |
| Waypoints / POIs | MEDIUM | MEDIUM | P2 |
| Expiry push notification | MEDIUM | LOW | P2 |
| Region/area browsing | LOW | LOW | P2 |
| Offline download prompt post-purchase | MEDIUM | LOW | P2 |
| Voice guidance | HIGH (perceived) | HIGH | P3 |
| Social / ratings | LOW | MEDIUM | P3 |
| iOS version | MEDIUM | HIGH | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

---

## Competitor Feature Analysis

| Feature | REVER (motorcycle) | onX Offroad | AllTrails (hiking) | Roadrunner approach |
|---------|-------------------|-------------|-------------------|---------------------|
| Route catalog | Community + curated (PRO) | Community + curated (staff-vetted) | Community | Admin-curated only (quality over quantity) |
| Difficulty rating | Basic | Detailed (vehicle type + grade) | Easy/Moderate/Hard | Easy/Moderate/Hard/Expert minimum |
| Offline maps | PRO feature (subscription) | All tiers (download areas) | Plus/Peak tier | All purchased routes (offline = safety) |
| Route preview before purchase | Yes (full open access, then navigation gated) | Yes | Yes | Preview map (line visible) + metadata; GPX data locked |
| Purchase model | Subscription (PRO) | Subscription (annual) | Subscription (Plus/Peak) | Per-route (day pass / rental / permanent) — unique model |
| License/content protection | None (GPX downloadable by PRO) | None (GPX downloadable) | None (GPX downloadable) | Encrypted on-device, never exportable — core differentiator |
| Voice navigation | Yes (PRO) | Yes | Yes (Plus) | No (v1 explicit out-of-scope) |
| Elevation profile | Yes | Yes | Yes | Dependent on GPX Z-values; v1.x |
| Ride stats during navigation | Yes (speed, distance) | Yes | Yes | v1.x |
| Waypoints / POIs | Yes (custom) | Yes (extensive) | Yes (waypoints) | Admin-defined in GPX; v1.x |
| Social / community | Strong (sharing, feed) | Moderate | Strong | Deliberately excluded v1 |

**Key observation:** No competitor in the motorcycle/offroad space uses a per-route licensing model with encrypted content protection. All competitors either give away route data freely (behind a subscription paywall for app features) or allow GPX export. Roadrunner's combination of per-route licensing + content encryption is unique and is the product's defensible moat.

---

## Sources

- [onX Offroad — 7 Essential Off-Road App Features](https://www.onxmaps.com/offroad/blog/essential-off-road-app-features) — LOW-MEDIUM confidence (marketing source, but feature list matches product)
- [REVER Motorcycle App — Google Play Store listing](https://play.google.com/store/apps/details?id=com.reverllc.rever) — MEDIUM confidence
- [Ride with GPS — Premium features](https://ridewithgps.com/premium) — MEDIUM confidence (official source, cycling not motorcycle)
- [Google Play Billing — One-time products with rental type](https://developer.android.com/google/play/billing/one-time-products) — HIGH confidence (official Android Developers docs)
- [Google Play Billing — Multiple purchase options and offers](https://developer.android.com/google/play/billing/one-time-product-multi-purchase-options-offers) — HIGH confidence
- [Adventure Cycling / Ride with GPS Experiences — offline licensed routes](https://www.adventurecycling.org/member_news/rolling-out-adventure-cycling-digital-routes-on-ride-with-gps/) — MEDIUM confidence (closest real-world analog to licensed route content model)
- [AllTrails — Navigate feature overview](https://support.alltrails.com/hc/en-us/articles/360059000272-The-Navigate-feature) — HIGH confidence (official support docs)
- [Riders Share — Best motorcycle route app comparison](https://www.riders-share.com/blog/article/is-there-an-app-for-motorcycle-routes) — LOW confidence (blog/editorial)
- [onX Offroad — Offline maps feature](https://www.onxmaps.com/offroad/app/features/offline-maps) — MEDIUM confidence

---
*Feature research for: Android GPX route navigation app with licensed/premium content (offroad motorcycle)*
*Researched: 2026-03-14*
