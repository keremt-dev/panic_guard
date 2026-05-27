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

## OEM coverage (record results)
- [ ] Pixel (stock Android)
- [ ] Samsung One UI
- [ ] Xiaomi MIUI / HyperOS
- [ ] Optionally: Huawei EMUI, OPPO ColorOS
