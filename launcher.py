"""
Roadrunner Dev Launcher
A GUI application to start/stop the full Roadrunner development environment.
Requires Python 3.9+ (uses tkinter, which ships with Python — no extra installs needed).
"""

import tkinter as tk
from tkinter import font as tkfont
import subprocess
import threading
import os
import sys
import time
import urllib.request
import urllib.error

# ─────────────────────────────────────────────────────────────
# Config
# ─────────────────────────────────────────────────────────────
PROJECT_ROOT   = os.path.dirname(os.path.abspath(__file__))
DOCKER_DESKTOP = r"C:\Program Files\Docker\Docker\Docker Desktop.exe"
EMULATOR_EXE   = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe")
ADB_EXE        = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
GRADLEW        = os.path.join(PROJECT_ROOT, "android", "gradlew.bat")
AVD_NAME       = "Medium_Phone_API_36.1"
APP_PACKAGE    = "com.roadrunner.app.motorcycle"
API_HEALTH_URL = "http://localhost:4000/health"
DASHBOARD_URL  = "http://localhost:4001"

# ─────────────────────────────────────────────────────────────
# Colours
# ─────────────────────────────────────────────────────────────
BG        = "#0D0D0D"
SURFACE   = "#1A1A1A"
SURFACE2  = "#242424"
BORDER    = "#3A3A3A"
ORANGE    = "#FF6D00"
ORANGE2   = "#FF9E40"
FG        = "#F5F5F5"
FG_DIM    = "#9E9E9E"
GREEN     = "#4CAF50"
RED       = "#F44336"
YELLOW    = "#FF9800"


# ─────────────────────────────────────────────────────────────
# Helper — run a shell command and stream output to a callback
# ─────────────────────────────────────────────────────────────
def run(cmd, cwd=None, log=print, shell=True):
    """Run a command, calling log(line) for each output line. Returns exit code."""
    proc = subprocess.Popen(
        cmd,
        cwd=cwd or PROJECT_ROOT,
        shell=shell,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    for line in proc.stdout:
        log(line.rstrip())
    proc.wait()
    return proc.returncode


def docker_running():
    try:
        result = subprocess.run(
            ["docker", "info"],
            capture_output=True, text=True, timeout=5,
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
        return result.returncode == 0
    except Exception:
        return False


def api_healthy():
    try:
        urllib.request.urlopen(API_HEALTH_URL, timeout=2)
        return True
    except Exception:
        return False


def emulator_running():
    try:
        result = subprocess.run(
            [ADB_EXE, "devices"],
            capture_output=True, text=True, timeout=5,
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
        return "emulator-" in result.stdout
    except Exception:
        return False


# ─────────────────────────────────────────────────────────────
# Main Application
# ─────────────────────────────────────────────────────────────
class RoadrunnerLauncher(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Roadrunner Launcher")
        self.configure(bg=BG)
        self.resizable(True, True)
        self.minsize(720, 580)

        # Track running background task
        self._busy = False

        self._build_ui()
        self._refresh_status()

    # ── UI construction ──────────────────────────────────────

    def _build_ui(self):
        # ── Header ──
        header = tk.Frame(self, bg=SURFACE, pady=14)
        header.pack(fill="x")

        tk.Label(
            header, text="●", fg=ORANGE, bg=SURFACE,
            font=("Segoe UI", 20),
        ).pack(side="left", padx=(20, 6))

        title_frame = tk.Frame(header, bg=SURFACE)
        title_frame.pack(side="left")
        tk.Label(title_frame, text="ROADRUNNER", fg=FG, bg=SURFACE,
                 font=("Segoe UI", 13, "bold")).pack(anchor="w")
        tk.Label(title_frame, text="Dev Launcher", fg=ORANGE, bg=SURFACE,
                 font=("Segoe UI", 8)).pack(anchor="w")

        # Refresh status button on right
        tk.Button(
            header, text="⟳  Refresh status", command=self._refresh_status,
            bg=SURFACE2, fg=FG_DIM, relief="flat", padx=10, pady=4,
            activebackground=BORDER, activeforeground=FG,
            font=("Segoe UI", 9), cursor="hand2",
        ).pack(side="right", padx=16)

        # ── Status panel ──
        status_frame = tk.LabelFrame(
            self, text="  Service Status  ", fg=FG_DIM, bg=BG,
            font=("Segoe UI", 9), bd=1, relief="solid",
            highlightbackground=BORDER,
        )
        status_frame.pack(fill="x", padx=16, pady=(14, 0))

        self._status_dots   = {}
        self._status_labels = {}
        services = [
            ("docker",    "Docker Desktop"),
            ("api",       "Backend API (port 4000)"),
            ("dashboard", "Admin Dashboard (port 4001)"),
            ("emulator",  "Android Emulator"),
        ]
        for i, (key, label) in enumerate(services):
            row = tk.Frame(status_frame, bg=BG, pady=5)
            row.grid(row=i // 2, column=i % 2, sticky="w", padx=18, pady=2)
            dot = tk.Label(row, text="●", fg=FG_DIM, bg=BG, font=("Segoe UI", 11))
            dot.pack(side="left", padx=(0, 8))
            lbl = tk.Label(row, text=label, fg=FG_DIM, bg=BG, font=("Segoe UI", 9))
            lbl.pack(side="left")
            self._status_dots[key]   = dot
            self._status_labels[key] = lbl

        status_frame.columnconfigure(0, weight=1)
        status_frame.columnconfigure(1, weight=1)

        # ── Action buttons ──
        btn_frame = tk.Frame(self, bg=BG)
        btn_frame.pack(fill="x", padx=16, pady=14)

        self._btn_full = self._make_button(
            btn_frame, "▶  Full Start  (rebuild + emulator + app)",
            ORANGE, BG, self._action_full_start,
        )
        self._btn_full.pack(fill="x", pady=(0, 6))

        row2 = tk.Frame(btn_frame, bg=BG)
        row2.pack(fill="x")
        self._btn_quick = self._make_button(
            row2, "▶  Quick Start  (no rebuild)",
            SURFACE2, FG, self._action_quick_start,
        )
        self._btn_quick.pack(side="left", fill="x", expand=True, padx=(0, 4))

        self._btn_rebuild = self._make_button(
            row2, "⟳  Rebuild Docker",
            SURFACE2, FG, self._action_rebuild,
        )
        self._btn_rebuild.pack(side="left", fill="x", expand=True, padx=(4, 0))

        row3 = tk.Frame(btn_frame, bg=BG)
        row3.pack(fill="x", pady=(6, 0))

        self._btn_browser = self._make_button(
            row3, "🌐  Open Dashboard",
            SURFACE2, FG, lambda: self._open_browser(),
        )
        self._btn_browser.pack(side="left", fill="x", expand=True, padx=(0, 4))

        self._btn_stop = self._make_button(
            row3, "■  Stop All  (docker compose down)",
            SURFACE2, RED, self._action_stop,
        )
        self._btn_stop.pack(side="left", fill="x", expand=True, padx=(4, 0))

        # ── Log output ──
        log_frame = tk.LabelFrame(
            self, text="  Output  ", fg=FG_DIM, bg=BG,
            font=("Segoe UI", 9), bd=1, relief="solid",
        )
        log_frame.pack(fill="both", expand=True, padx=16, pady=(0, 16))

        self._log = tk.Text(
            log_frame, bg="#0A0A0A", fg=FG, font=("Consolas", 9),
            relief="flat", padx=10, pady=8, wrap="word",
            insertbackground=ORANGE,
        )
        self._log.pack(side="left", fill="both", expand=True)

        scrollbar = tk.Scrollbar(log_frame, command=self._log.yview, bg=SURFACE)
        scrollbar.pack(side="right", fill="y")
        self._log.config(yscrollcommand=scrollbar.set)

        # Tag styles for log
        self._log.tag_config("step",  foreground=ORANGE,  font=("Consolas", 9, "bold"))
        self._log.tag_config("ok",    foreground=GREEN)
        self._log.tag_config("warn",  foreground=YELLOW)
        self._log.tag_config("error", foreground=RED)
        self._log.tag_config("dim",   foreground=FG_DIM)

        # Status bar
        self._statusbar = tk.Label(
            self, text="Ready.", fg=FG_DIM, bg=SURFACE,
            font=("Segoe UI", 8), anchor="w", padx=12, pady=4,
        )
        self._statusbar.pack(fill="x", side="bottom")

    def _make_button(self, parent, text, bg, fg, cmd):
        return tk.Button(
            parent, text=text, command=cmd,
            bg=bg, fg=fg, relief="flat",
            font=("Segoe UI", 9, "bold"),
            padx=12, pady=8, cursor="hand2",
            activebackground=BORDER, activeforeground=FG,
        )

    # ── Logging ──────────────────────────────────────────────

    def _log_line(self, text, tag=None):
        self._log.configure(state="normal")
        if tag:
            self._log.insert("end", text + "\n", tag)
        else:
            self._log.insert("end", text + "\n")
        self._log.see("end")
        self._log.configure(state="disabled")

    def _log_step(self, msg):
        self._log_line(f"\n── {msg}", "step")
        self._statusbar.config(text=msg)

    def _log_ok(self, msg):
        self._log_line(f"  ✓  {msg}", "ok")

    def _log_warn(self, msg):
        self._log_line(f"  ⚠  {msg}", "warn")

    def _log_err(self, msg):
        self._log_line(f"  ✗  {msg}", "error")

    def _log_out(self, line):
        self._log_line(f"     {line}", "dim")

    # ── Status polling ───────────────────────────────────────

    def _refresh_status(self):
        def check():
            # Docker
            d = docker_running()
            a = api_healthy() if d else False
            # For dashboard, just check if port 4001 responds
            db = False
            try:
                urllib.request.urlopen("http://localhost:4001", timeout=2)
                db = True
            except Exception:
                pass
            e = emulator_running()
            self.after(0, lambda: self._apply_status(d, a, db, e))

        threading.Thread(target=check, daemon=True).start()

    def _apply_status(self, docker, api, dash, emu):
        def dot(key, ok, partial=False):
            color = GREEN if ok else (YELLOW if partial else RED)
            label_color = FG if ok else FG_DIM
            self._status_dots[key].config(fg=color)
            self._status_labels[key].config(fg=label_color)

        dot("docker",    docker)
        dot("api",       api)
        dot("dashboard", dash)
        dot("emulator",  emu)

    # ── Actions ──────────────────────────────────────────────

    def _set_busy(self, busy):
        self._busy = busy
        state = "disabled" if busy else "normal"
        for btn in [self._btn_full, self._btn_quick, self._btn_rebuild,
                    self._btn_stop, self._btn_browser]:
            btn.config(state=state)
        if not busy:
            self._statusbar.config(text="Done.")
            self._refresh_status()

    def _run_task(self, fn):
        if self._busy:
            self._log_warn("Another task is already running. Please wait.")
            return
        self._log.configure(state="normal")
        self._log.delete("1.0", "end")
        self._log.configure(state="disabled")
        self._set_busy(True)
        threading.Thread(target=fn, daemon=True).start()

    # Full start ────────────────────────────────────────────

    def _action_full_start(self):
        self._run_task(self._task_full_start)

    def _task_full_start(self):
        try:
            self._ensure_docker()
            self._docker_compose_up(rebuild=True)
            self._wait_for_api()
            self._run_migrations()
            self._start_emulator()
            self._build_and_install()
            self._open_browser()
            self._log_ok("Full start complete!")
        except Exception as e:
            self._log_err(f"Task failed: {e}")
        finally:
            self.after(0, lambda: self._set_busy(False))

    # Quick start ───────────────────────────────────────────

    def _action_quick_start(self):
        self._run_task(self._task_quick_start)

    def _task_quick_start(self):
        try:
            self._ensure_docker()
            self._docker_compose_up(rebuild=False)
            self._wait_for_api()
            self._run_migrations()
            self._start_emulator()
            self._open_browser()
            self._log_ok("Quick start complete! App is already installed on the emulator.")
        except Exception as e:
            self._log_err(f"Task failed: {e}")
        finally:
            self.after(0, lambda: self._set_busy(False))

    # Rebuild ───────────────────────────────────────────────

    def _action_rebuild(self):
        self._run_task(self._task_rebuild)

    def _task_rebuild(self):
        try:
            self._ensure_docker()
            self._docker_compose_up(rebuild=True)
            self._wait_for_api()
            self._run_migrations()
            self._log_ok("Rebuild complete.")
        except Exception as e:
            self._log_err(f"Rebuild failed: {e}")
        finally:
            self.after(0, lambda: self._set_busy(False))

    # Stop ──────────────────────────────────────────────────

    def _action_stop(self):
        self._run_task(self._task_stop)

    def _task_stop(self):
        try:
            self._log_step("Stopping all services (docker compose down)...")
            code = run("docker compose down", log=self._log_out)
            if code == 0:
                self._log_ok("All Docker services stopped.")
            else:
                self._log_warn("docker compose down returned a non-zero exit code.")
        except Exception as e:
            self._log_err(str(e))
        finally:
            self.after(0, lambda: self._set_busy(False))

    # ── Step implementations ─────────────────────────────────

    def _ensure_docker(self):
        self._log_step("Checking Docker Desktop...")
        if docker_running():
            self._log_ok("Docker is running.")
            return

        self._log_warn("Docker Desktop is not running — starting it...")
        subprocess.Popen([DOCKER_DESKTOP], creationflags=subprocess.CREATE_NO_WINDOW)

        for elapsed in range(0, 91, 3):
            time.sleep(3)
            if docker_running():
                self._log_ok("Docker started.")
                return
            self._log_out(f"Waiting... {elapsed + 3}s")

        raise RuntimeError("Docker Desktop did not start within 90 seconds.")

    def _docker_compose_up(self, rebuild=False):
        flag = "--build " if rebuild else ""
        label = "Building and starting" if rebuild else "Starting"
        self._log_step(f"{label} services (docker compose up -d {flag.strip()})...")
        cmd = f"docker compose up -d {flag}".strip()
        code = run(cmd, log=self._log_out)
        if code != 0:
            raise RuntimeError("docker compose up failed.")
        self._log_ok("Services started.")

    def _wait_for_api(self):
        self._log_step("Waiting for API health check...")
        for elapsed in range(0, 61, 2):
            time.sleep(2)
            if api_healthy():
                self._log_ok(f"API is healthy at {API_HEALTH_URL}")
                return
            self._log_out(f"Waiting... {elapsed + 2}s")
        self._log_warn("API health check timed out — it may still be starting.")

    def _run_migrations(self):
        self._log_step("Running database migrations...")
        code = run("docker compose exec api npx prisma migrate deploy", log=self._log_out)
        if code == 0:
            self._log_ok("Migrations applied.")
        else:
            self._log_warn("Migration command returned non-zero — check output above.")

    def _start_emulator(self):
        self._log_step(f"Starting Android emulator ({AVD_NAME})...")
        if emulator_running():
            self._log_ok("Emulator already running.")
            return

        subprocess.Popen(
            [EMULATOR_EXE, "-avd", AVD_NAME, "-no-snapshot-load"],
            creationflags=subprocess.CREATE_NO_WINDOW,
        )

        self._log_out("Waiting for emulator to boot (up to 120 seconds)...")
        for elapsed in range(0, 121, 5):
            time.sleep(5)
            try:
                result = subprocess.run(
                    [ADB_EXE, "-e", "shell", "getprop", "sys.boot_completed"],
                    capture_output=True, text=True, timeout=5,
                    creationflags=subprocess.CREATE_NO_WINDOW,
                )
                if result.stdout.strip() == "1":
                    # Dismiss lock screen
                    subprocess.run([ADB_EXE, "-e", "shell", "input", "keyevent", "82"],
                                   capture_output=True, creationflags=subprocess.CREATE_NO_WINDOW)
                    self._log_ok("Emulator booted.")
                    return
            except Exception:
                pass
            self._log_out(f"Waiting... {elapsed + 5}s")

        self._log_warn("Emulator boot timed out — it may still be starting in background.")

    def _build_and_install(self):
        self._log_step("Building Android app (motorcycleDebug) — this takes a few minutes...")
        code = run(f'"{GRADLEW}" installMotorcycleDebug', log=self._log_out)
        if code != 0:
            raise RuntimeError("Gradle build failed. See output above.")
        self._log_ok("App installed.")

        # Launch the app
        self._log_out(f"Launching {APP_PACKAGE}...")
        subprocess.run(
            [ADB_EXE, "shell", "monkey", "-p", APP_PACKAGE, "1"],
            capture_output=True, creationflags=subprocess.CREATE_NO_WINDOW,
        )
        self._log_ok("App launched on emulator.")

    def _open_browser(self):
        self._log_step("Opening admin dashboard in browser...")
        import webbrowser
        webbrowser.open(DASHBOARD_URL)
        self._log_ok(f"Browser opened at {DASHBOARD_URL}")


# ─────────────────────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────────────────────
if __name__ == "__main__":
    app = RoadrunnerLauncher()

    # Center the window on screen
    app.update_idletasks()
    w, h = 760, 640
    sw = app.winfo_screenwidth()
    sh = app.winfo_screenheight()
    app.geometry(f"{w}x{h}+{(sw - w) // 2}+{(sh - h) // 2}")

    app.mainloop()
