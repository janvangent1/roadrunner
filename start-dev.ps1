# Roadrunner — Local Development Startup Script
# Starts Docker, backend, dashboard, Android emulator, and builds the app.
# Run from the project root: .\start-dev.ps1
# Optional flags:
#   -SkipBuild   Skip Android app build (use if app is already installed)
#   -SkipEmulator  Skip starting the Android emulator

param(
    [switch]$SkipBuild,
    [switch]$SkipEmulator
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$EmulatorPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\emulator\emulator.exe"
$AdbPath     = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$AvdName     = "Medium_Phone_API_36.1"
$DashboardUrl = "http://localhost:4001"
$ApiHealthUrl = "http://localhost:4000/health"

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "  >> $msg" -ForegroundColor Cyan
}

function Write-Ok([string]$msg) {
    Write-Host "  OK  $msg" -ForegroundColor Green
}

function Write-Warn([string]$msg) {
    Write-Host "  !!  $msg" -ForegroundColor Yellow
}

# ─────────────────────────────────────────────────────────────
# 1. Make sure we're in the project root
# ─────────────────────────────────────────────────────────────
Set-Location $ProjectRoot

Write-Host ""
Write-Host "  =====================================" -ForegroundColor DarkCyan
Write-Host "   ROADRUNNER — Dev Environment Startup" -ForegroundColor Cyan
Write-Host "  =====================================" -ForegroundColor DarkCyan

# ─────────────────────────────────────────────────────────────
# 2. Start Docker Desktop if not running
# ─────────────────────────────────────────────────────────────
Write-Step "Checking Docker Desktop..."

$dockerRunning = $false
try {
    $null = & docker info 2>&1
    if ($LASTEXITCODE -eq 0) { $dockerRunning = $true }
} catch {}

if (-not $dockerRunning) {
    Write-Warn "Docker Desktop is not running. Starting it..."
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    Write-Host "  Waiting for Docker to start (up to 90 seconds)..." -ForegroundColor DarkGray

    $timeout = 90
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        Start-Sleep -Seconds 3
        $elapsed += 3
        try {
            $null = & docker info 2>&1
            if ($LASTEXITCODE -eq 0) { $dockerRunning = $true; break }
        } catch {}
        Write-Host "  ... $elapsed s" -ForegroundColor DarkGray
    }

    if (-not $dockerRunning) {
        Write-Host ""
        Write-Host "  ERROR: Docker did not start within 90 seconds. Please start Docker Desktop manually and re-run this script." -ForegroundColor Red
        exit 1
    }
}
Write-Ok "Docker is running."

# ─────────────────────────────────────────────────────────────
# 3. Start backend + dashboard with docker compose
# ─────────────────────────────────────────────────────────────
Write-Step "Starting backend + dashboard (docker compose)..."

& docker compose up -d 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: docker compose failed. Check output above." -ForegroundColor Red
    exit 1
}

# ─────────────────────────────────────────────────────────────
# 4. Wait for API health check
# ─────────────────────────────────────────────────────────────
Write-Step "Waiting for API to be healthy..."

$timeout = 60
$elapsed = 0
$healthy = $false
while ($elapsed -lt $timeout) {
    Start-Sleep -Seconds 2
    $elapsed += 2
    try {
        $response = Invoke-WebRequest -Uri $ApiHealthUrl -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) { $healthy = $true; break }
    } catch {}
    Write-Host "  ... $elapsed s" -ForegroundColor DarkGray
}

if (-not $healthy) {
    Write-Warn "API health check timed out. It may still be starting — check: $ApiHealthUrl"
} else {
    Write-Ok "API is healthy at $ApiHealthUrl"
}

# ─────────────────────────────────────────────────────────────
# 5. Run database migrations
# ─────────────────────────────────────────────────────────────
Write-Step "Running database migrations..."
& docker compose exec api npx prisma migrate deploy 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
Write-Ok "Migrations applied."

# ─────────────────────────────────────────────────────────────
# 6. Start Android emulator
# ─────────────────────────────────────────────────────────────
if (-not $SkipEmulator) {
    Write-Step "Starting Android emulator ($AvdName)..."

    # Check if emulator is already running
    $adbDevices = & $AdbPath devices 2>&1
    $emulatorAlreadyRunning = $adbDevices -match "emulator-\d+"

    if ($emulatorAlreadyRunning) {
        Write-Ok "Emulator already running."
    } else {
        # Start emulator in background
        Start-Process -FilePath $EmulatorPath `
            -ArgumentList "-avd", $AvdName, "-no-snapshot-load" `
            -WindowStyle Normal

        Write-Host "  Waiting for emulator to boot (up to 120 seconds)..." -ForegroundColor DarkGray
        $timeout = 120
        $elapsed = 0
        $booted = $false
        while ($elapsed -lt $timeout) {
            Start-Sleep -Seconds 5
            $elapsed += 5
            try {
                $bootProp = & $AdbPath -e shell getprop sys.boot_completed 2>&1
                if ($bootProp -match "1") { $booted = $true; break }
            } catch {}
            Write-Host "  ... $elapsed s" -ForegroundColor DarkGray
        }

        if ($booted) {
            Write-Ok "Emulator booted."
            # Dismiss any lock screen
            & $AdbPath -e shell input keyevent 82 2>&1 | Out-Null
        } else {
            Write-Warn "Emulator boot timed out. It may still be starting in the background."
        }
    }
} else {
    Write-Warn "Skipping emulator (--SkipEmulator flag set)."
}

# ─────────────────────────────────────────────────────────────
# 7. Build and install Android app
# ─────────────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Step "Building and installing Android app (motorcycleDebug)..."
    Write-Host "  This may take 5-10 minutes on first build." -ForegroundColor DarkGray

    $gradlePath = Join-Path $ProjectRoot "android\gradlew.bat"
    & $gradlePath installMotorcycleDebug 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }

    if ($LASTEXITCODE -eq 0) {
        Write-Ok "App installed on emulator."
        # Launch the app
        & $AdbPath shell monkey -p com.roadrunner.app.motorcycle 1 2>&1 | Out-Null
        Write-Ok "App launched."
    } else {
        Write-Host "  ERROR: Gradle build failed. Check output above." -ForegroundColor Red
    }
} else {
    Write-Warn "Skipping Android build (--SkipBuild flag set)."
}

# ─────────────────────────────────────────────────────────────
# 8. Open admin dashboard in browser
# ─────────────────────────────────────────────────────────────
Write-Step "Opening admin dashboard in browser..."
Start-Process $DashboardUrl
Write-Ok "Browser opened at $DashboardUrl"

# ─────────────────────────────────────────────────────────────
# Done
# ─────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "  =====================================" -ForegroundColor DarkCyan
Write-Host "   All done! Services running:" -ForegroundColor Green
Write-Host "   Backend API  : http://localhost:4000" -ForegroundColor White
Write-Host "   Admin panel  : http://localhost:4001" -ForegroundColor White
Write-Host "   Emulator     : Android emulator" -ForegroundColor White
Write-Host ""
Write-Host "   To stop everything:" -ForegroundColor DarkGray
Write-Host "   docker compose down" -ForegroundColor DarkGray
Write-Host "  =====================================" -ForegroundColor DarkCyan
Write-Host ""
