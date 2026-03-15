# Roadrunner — Setup Guide

This guide walks you through getting Roadrunner running from scratch: backend on your Raspberry Pi, admin dashboard, and the Android app on a real device.

---

## Prerequisites

| Tool | Version | Where to get it |
|------|---------|-----------------|
| Docker + Docker Compose | Latest | https://docs.docker.com/engine/install/raspberry-pi-os/ |
| Node.js | 20+ | https://nodejs.org (for local dashboard dev only) |
| Android Studio | Hedgehog+ | https://developer.android.com/studio |
| Cloudflare account | Free | https://dash.cloudflare.com (for public tunnel) |
| Google Cloud Console account | Free | https://console.cloud.google.com |
| Google Play Console account | $25 one-time | https://play.google.com/console (for Play Integrity) |

---

## Part 1 — Backend (Raspberry Pi)

### 1.1 Clone the repo onto the Pi

```bash
git clone <your-repo-url> roadrunner
cd roadrunner/backend
```

### 1.2 Create the environment file

```bash
cp .env.example .env
nano .env
```

Fill in every value:

```env
# Database
DATABASE_URL="postgresql://roadrunner:yourpassword@postgres:5432/roadrunner"
POSTGRES_USER=roadrunner
POSTGRES_PASSWORD=yourpassword          # choose a strong password
POSTGRES_DB=roadrunner

# Redis
REDIS_URL="redis://redis:6379"

# JWT — generate with: node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"
JWT_SECRET=<64-char random hex>
JWT_REFRESH_SECRET=<64-char random hex>
JWT_EXPIRES_IN=15m
JWT_REFRESH_EXPIRES_IN=30d

# Google OAuth (from Google Cloud Console — see Part 3)
GOOGLE_CLIENT_ID=<your-web-client-id>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<your-client-secret>

# Tink encryption key — see Part 2 below for how to generate this
TINK_KEYSET_JSON=<base64-encoded binary keyset>

# Play Integrity (from Google Play Console — see Part 4)
PLAY_INTEGRITY_DECRYPTION_KEY=<base64-decryption-key>
PLAY_INTEGRITY_VERIFICATION_KEY=<base64-verification-key>
ANDROID_PACKAGE_NAME=com.roadrunner.app

# Admin account (created on first run)
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_PASSWORD=<strong-password>
```

### 1.3 Start the backend

```bash
docker compose up -d
```

On first run this will:
- Pull PostgreSQL 16 and Redis 7 images
- Build the Fastify API container
- Run Prisma migrations automatically (`prisma migrate deploy`)

Verify it's running:

```bash
docker compose ps          # all three services should show "Up"
curl http://localhost:3000/health   # should return {"status":"ok"}
```

### 1.4 Create the first admin user

```bash
docker compose exec api node -e "
const {PrismaClient} = require('@prisma/client');
const bcrypt = require('bcryptjs');
const p = new PrismaClient();
p.user.create({data:{email:'admin@yourdomain.com', passwordHash: bcrypt.hashSync('yourpassword',10), role:'ADMIN'}}).then(u=>console.log('Created:',u.email)).finally(()=>p.\$disconnect());
"
```

---

## Part 2 — Generate the Tink Encryption Key

The Tink keyset is used to encrypt GPX files on the server and decrypt them in the app. You generate it once and never change it (or all encrypted GPX files become unreadable).

### 2.1 Generate the keyset

```bash
cd backend
npm install          # if not already done
node -e "
const tink = require('tink-crypto');
// Generate a new AES-256-GCM keyset
const keyset = tink.generateKeyset('AES256_GCM');
const b64 = Buffer.from(keyset).toString('base64');
console.log('TINK_KEYSET_JSON=' + b64);
"
```

Copy the output value into `backend/.env` as `TINK_KEYSET_JSON`.

### 2.2 Put the same key in the Android app

Open [android/app/build.gradle.kts](android/app/build.gradle.kts) and replace the debug placeholder:

```kotlin
debug {
    buildConfigField("String", "TINK_KEYSET_B64", "\"YOUR_BASE64_KEYSET_HERE\"")
```

> The debug build uses this key directly. The release build intentionally omits `TINK_KEYSET_B64` — for production you should deliver the key via a secure endpoint authenticated by the license session token (v2 feature). For v1 release, add a `release` `buildConfigField` with the same value.

---

## Part 3 — Google Sign-In Setup

### 3.1 Create an OAuth 2.0 Client

1. Go to [Google Cloud Console](https://console.cloud.google.com) → APIs & Services → Credentials
2. Create project (or use existing)
3. Click **Create Credentials** → OAuth 2.0 Client ID
4. Application type: **Web application**
5. Name it "Roadrunner Backend"
6. Authorized redirect URIs: `https://your-cloudflare-tunnel-url/api/v1/auth/google/callback`
7. Copy the **Client ID** and **Client Secret** into `backend/.env`

### 3.2 Create an Android OAuth Client

1. Same Credentials page → **Create Credentials** → OAuth 2.0 Client ID
2. Application type: **Android**
3. Package name: `com.roadrunner.app`
4. SHA-1 fingerprint: run `./gradlew signingReport` in `android/` and copy the debug SHA-1
5. Copy the resulting **Web Client ID** (not the Android client ID) into:

```xml
<!-- android/app/src/main/res/values/strings.xml -->
<string name="google_server_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
```

---

## Part 4 — Play Integrity API Setup

This verifies devices haven't been rooted or tampered with before granting access to route content.

### 4.1 Link your app to Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Create an app (or use existing) with package name `com.roadrunner.app`
3. Go to **Setup** → **App integrity**
4. Note your **Cloud project number** — put it in:

```xml
<!-- android/app/src/main/res/values/strings.xml -->
<string name="play_integrity_cloud_project_number">YOUR_PROJECT_NUMBER</string>
```

### 4.2 Get the Play Integrity API credentials

1. In your Google Cloud project, enable the **Play Integrity API**
2. Go to **Play Console** → App integrity → **Download encryption keys**
3. Download the decryption and verification keys
4. Add them to `backend/.env`:

```env
PLAY_INTEGRITY_DECRYPTION_KEY=<base64-decryption-key>
PLAY_INTEGRITY_VERIFICATION_KEY=<base64-verification-key>
```

---

## Part 5 — Cloudflare Tunnel (Public URL for the Pi)

This gives your Raspberry Pi a public HTTPS URL so the app works when riders are away from your home network.

### 5.1 Install cloudflared on the Pi

```bash
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 -o cloudflared
chmod +x cloudflared
sudo mv cloudflared /usr/local/bin/
```

### 5.2 Authenticate and create the tunnel

```bash
cloudflared tunnel login          # opens a browser, authorize with your Cloudflare account
cloudflared tunnel create roadrunner-api
cloudflared tunnel route dns roadrunner-api api.yourdomain.com
```

### 5.3 Create tunnel config

```bash
nano ~/.cloudflared/config.yml
```

```yaml
tunnel: <your-tunnel-id>
credentials-file: /home/pi/.cloudflared/<your-tunnel-id>.json

ingress:
  - hostname: api.yourdomain.com
    service: http://localhost:3000
  - service: http_status:404
```

### 5.4 Run as a service

```bash
sudo cloudflared service install
sudo systemctl enable cloudflared
sudo systemctl start cloudflared
```

Verify: `curl https://api.yourdomain.com/health` should return `{"status":"ok"}`

### 5.5 Update the Android app base URL

Open [android/app/build.gradle.kts](android/app/build.gradle.kts):

```kotlin
release {
    buildConfigField("String", "BASE_URL", "\"https://api.yourdomain.com\"")
```

### 5.6 Get the certificate pin

```bash
# Get the SHA-256 pin of the Cloudflare certificate your tunnel uses
echo | openssl s_client -connect api.yourdomain.com:443 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | base64
```

Paste the result into [android/app/build.gradle.kts](android/app/build.gradle.kts):

```kotlin
release {
    buildConfigField("String", "CERT_PIN_SHA256", "\"sha256/YOUR_BASE64_PIN_HERE\"")
```

---

## Part 6 — Admin Dashboard

### 6.1 Local development

```bash
cd dashboard
npm install
npm run dev     # http://localhost:3001
```

Set the API URL in `dashboard/.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:3000
```

### 6.2 Deploy on the Pi (alongside the backend)

The dashboard is included in `docker-compose.yml`. It builds as a standalone Next.js app:

```bash
# From repo root on the Pi
docker compose up -d dashboard
```

Add a second Cloudflare tunnel ingress for the dashboard if you want it publicly accessible:

```yaml
  - hostname: admin.yourdomain.com
    service: http://localhost:3001
```

---

## Part 7 — Build and Install the Android App

### 7.1 Debug build (for testing on your own device)

```bash
cd android
./gradlew installMotorcycleDebug
```

This installs the debug APK directly to a connected Android device or emulator.

### 7.2 Release build (for distribution)

First, create a signing keystore if you don't have one:

```bash
keytool -genkey -v -keystore roadrunner-release.keystore \
  -alias roadrunner -keyalg RSA -keysize 2048 -validity 10000
```

Add signing config to [android/app/build.gradle.kts](android/app/build.gradle.kts):

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../../roadrunner-release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "roadrunner"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... rest of release config
    }
}
```

Then build:

```bash
KEYSTORE_PASSWORD=yourpass KEY_PASSWORD=yourpass \
  ./gradlew bundleMotorcycleRelease    # .aab for Play Store
# or
  ./gradlew assembleMotorcycleRelease  # .apk for sideload
```

---

## Part 8 — First Run Checklist

Before handing the app to riders, verify these end-to-end:

- [ ] Backend health: `curl https://api.yourdomain.com/health` returns `{"status":"ok"}`
- [ ] Admin login works at `https://admin.yourdomain.com`
- [ ] Upload a test GPX route via the dashboard
- [ ] Grant yourself a license via the dashboard (Licenses → New)
- [ ] Install the app on your phone
- [ ] Log in, browse catalog — test route appears
- [ ] License status shows "Owned" on the route card
- [ ] Tap "Start Navigation" — map loads with route polyline
- [ ] GPS position dot appears on the map
- [ ] HUD shows speed/distance/time
- [ ] Download offline tiles on the route detail page
- [ ] Enable airplane mode — map still works

---

## Quick Reference

| Service | Local URL | Public URL |
|---------|-----------|------------|
| Backend API | `http://pi-ip:3000` | `https://api.yourdomain.com` |
| Admin Dashboard | `http://pi-ip:3001` | `https://admin.yourdomain.com` |
| PostgreSQL | `pi-ip:5432` | Not exposed |
| Redis | `pi-ip:6379` | Not exposed |

### Useful commands

```bash
# View backend logs
docker compose logs -f api

# Restart backend after .env change
docker compose restart api

# Run database migrations after schema change
docker compose exec api npx prisma migrate deploy

# Backup the database
docker compose exec postgres pg_dump -U roadrunner roadrunner > backup.sql
```
