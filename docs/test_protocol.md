# AutoFit Test Protocol (Phase 1)

Research smoke and full-run checklist for Sprint 7 validation. Record device model, Android API level, and app version (`versionName`) for each run.

## Prerequisites

- Health Connect `WRITE_STEPS` granted
- Activity recognition granted (Android 14+ health FGS)
- Battery optimization ignored (recommended for long runs)
- Optional: Display over other apps (overlay chip)

## Test Case A — Screen Off

| Field | Value |
|-------|-------|
| Config | 120 SPM ±15, 60 min, batch 1/3/5 |
| Condition | Start experiment, turn screen off |
| Smoke duration | 15 min |
| Observe | FGS alive, heartbeat count increases, notification/overlay updates |

**Pass:** Heartbeats continue; no `STOPPED` / `INTERRUPTED_BY_REBOOT` before duration ends.

## Test Case B — Battery Saver

| Field | Value |
|-------|-------|
| Config | 120 SPM ±15, 60 min |
| Condition | Enable Battery Saver before start |
| Smoke duration | 15 min |
| Observe | Missed heartbeats, service restart, write failures |

**Pass:** Experiment remains `RUNNING` or ends with documented status; failures logged in History.

## Test Case C — Task Removal

| Field | Value |
|-------|-------|
| Config | 120 SPM ±15, 10 min |
| Condition | Start, swipe app away from recents |
| Duration | 10 min |
| Observe | FGS still running, notification persists |

**Pass:** `RUNNING` status; heartbeats continue after reopening app.

## Test Case D — Reboot

| Field | Value |
|-------|-------|
| Config | 120 SPM ±15, 30+ min |
| Condition | Reboot device during experiment |
| Observe | History status after boot |

**Pass:** Experiment marked `INTERRUPTED_BY_REBOOT` with result row (no fake FGS auto-resume).

## API Matrix (NFR-004)

| API | Emulator smoke | Notes |
|-----|----------------|-------|
| 31 (Android 12) | Config → Start → 3 min | HC APK may need install via Environment |
| 33 (Android 13) | POST_NOTIFICATIONS grant/deny | Notification optional path |
| 34+ (Android 14) | Health FGS + ACTIVITY_RECOGNITION | Built-in HC |

## Overlay paths

1. **Granted:** status chip updates once per minute (aligned with notification throttle)
2. **Denied:** service runs without crash; notification only

## Repeatability (NFR-002)

Run the same config twice; History must show two comparable `COMPLETED` rows with independent `experimentId`.
