# Panic Shield — Manual QA Checklist

Run before every Closed Test build. ~10 minutes on a real device.

## Setup
- [ ] Fresh install (uninstall any prior version)
- [ ] First launch shows onboarding (3 pages)
- [ ] "Continue" opens Accessibility settings
- [ ] Enable Panic Shield in Accessibility settings → return to app
- [ ] Home screen shows "Active"

## Core trigger
- [ ] App in foreground → Volume Up x3 within 2s → screen locks immediately, vibration buzzes
- [ ] Home screen (launcher) → Volume Up x3 within 2s → screen locks
- [ ] Other app foreground (Chrome) → Volume Up x3 within 2s → screen locks
- [ ] Music playing → Volume Up x3 → screen locks; volume continues to rise normally (event is NOT consumed)
- [ ] Lock the device manually first → press Volume Up x3 → confirms accessibility services do not run on lockscreen (known limit; not a regression)

## Negative cases
- [ ] Volume Up x2 → no lock
- [ ] Volume Up x4 spread over 5s → no lock
- [ ] Volume Up x3 with first press held (repeatCount > 0) → no lock
- [ ] Volume DOWN x3 → no lock
- [ ] Disable in app Settings → trigger fires → no lock (verifies enabled flag)

## Settings
- [ ] Slider: press count 2..5 → reflected in trigger behavior on next test
- [ ] Slider: window 1.0..4.0s → reflected
- [ ] Toggle vibrate off → trigger fires without buzz
- [ ] Force-kill app → cold start → settings persist

## Reliability
- [ ] Force-kill app from recents → trigger still fires (service is system-bound, survives)
- [ ] Restart device → re-enable Accessibility OR confirm it persists (some OEMs reset it) → document outcome per OEM
- [ ] Doze mode (leave device idle 30 min) → trigger still works
- [ ] Battery-optimization exemption granted → no MIUI/HyperOS / EMUI auto-disable after 24h

## v1.1 — SOS SMS
- [ ] Settings → pick emergency contact → grant SMS + location prompts
- [ ] Trigger panic → contact receives one SMS with the fixed text + a working Maps link
- [ ] No contact set → trigger → no SMS, no crash
- [ ] Airplane mode → trigger → no crash; SMS queued/failed gracefully

## v1.1 — Silent capture
- [ ] Capture toggle on + camera permission + "Display over other apps" granted
- [ ] Trigger → lock → wake/unlock → a photo of the person appears in Captured photos
- [ ] Capture toggle off → trigger → no photo, no camera notification
- [ ] Photos are app-private (not in the device gallery); delete works

## v1.1 — Protected apps blocking
- [ ] Mark an app protected; trigger panic
- [ ] Open the protected app → bounced to home immediately
- [ ] Open a non-protected app → opens normally
- [ ] Disarm → protected app opens normally again

## v1.1 — Safe PIN / disarm
- [ ] Set a safe PIN in Settings
- [ ] Trigger → app shows PANIC ACTIVE; wrong PIN → error; correct PIN → disarms
- [ ] No PIN set → PANIC ACTIVE shows a one-tap Disarm
- [ ] Force-kill during panic → reopen → still PANIC ACTIVE (state persisted)

## OEM coverage (record results)
- [ ] Pixel (stock Android)
- [ ] Samsung One UI
- [ ] Xiaomi MIUI / HyperOS
- [ ] Optionally: Huawei EMUI, OPPO ColorOS
