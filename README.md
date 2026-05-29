# Panic Shield

**Volume up three times. Lock, alert, and protect — instantly.**

Panic Shield is an Android personal-safety app. A discreet triple press of the
Volume Up key locks the phone and fires the safeguards you've armed: it texts a
trusted contact your location, photographs whoever next unlocks the device, and
walls off your sensitive apps until you enter a safe PIN.

> **Privacy first:** Panic Shield has **no `INTERNET` permission** and **no
> backend**. Nothing is ever uploaded to us or any server. The only data that
> leaves your phone is an SOS text *you* trigger, sent through your own carrier
> to the contact *you* chose.

---

## What it does

| Feature | Behavior |
|---|---|
| ⚡ **Instant lock** | Volume Up ×3 (count + time window configurable) locks the screen via the Accessibility Service. |
| 📍 **SOS SMS** | On trigger, sends a fixed safety message + a Google Maps location link to your chosen emergency contact. |
| 📷 **Silent capture** | When someone wakes/unlocks the phone *while panic is active*, the front camera quietly saves a photo to app-private storage. |
| 🔒 **Protected apps** | While panic is active, opening a protected app (banking, gallery, messengers…) bounces it straight back to the home screen. |
| 🛡️ **Safe-PIN disarm** | Panic stays active — and protected apps stay blocked — until you enter your safe PIN. |

The whole panic flow:

```
Volume Up ×3  ─►  lock screen  +  SOS SMS  +  arm safeguards
                                      │
   attacker wakes / unlocks the phone │
                                      ▼
            silent front-camera photo  +  protected apps blocked
                                      │
                you enter safe PIN     ▼
                          ─►  disarm, back to normal
```

---

## Why not just the power button?

The OS power button only locks the screen. Panic Shield adds the things it
can't: a one-handed in-pocket trigger, an automatic SOS with your location, a
photo of whoever ends up holding your phone, and a barrier in front of your
bank apps even after the device is unlocked.

---

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), always-dark UI with a
  Canvas-drawn shield emblem — no raster assets.
- **AccessibilityService** — volume-key trigger, screen lock, app blocking.
- **CameraX** + **ML Kit face detection** — silent capture from the analysis
  stream (works where still-capture doesn't, e.g. emulator webcam).
- **DataStore (Preferences)** — all settings, panic state, and the salted
  PBKDF2 safe-PIN hash, stored locally.
- **FusedLocationProvider** — one location fix at panic time for the SOS SMS.
- `minSdk 28`, `targetSdk 35`.

## Build

1. Open the project in **Android Studio** (uses its bundled JDK 17).
2. Let Gradle sync, then **Run** (`./gradlew assembleDebug` from the bundled JDK
   also works; the system JDK must be 17+).
3. On first launch, grant the Accessibility permission via the onboarding deep
   link, then optionally set an emergency contact, camera/overlay permissions,
   and a safe PIN in Settings.

## Permissions

| Permission | Purpose |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Volume-key trigger, screen lock, app blocking |
| `SEND_SMS` | Send the SOS message to your chosen contact |
| `ACCESS_FINE_LOCATION` | Location link in the SOS SMS (read once at trigger) |
| `CAMERA` | Silent safety photo on unlock |
| `READ_CONTACTS` | System contact picker for the emergency contact |
| `QUERY_ALL_PACKAGES` | Let you pick which installed apps to protect |
| `SYSTEM_ALERT_WINDOW` | Background-start exemption for the capture service |
| `POST_NOTIFICATIONS`, `VIBRATE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Foreground-service notice, haptic, reliability |

No `INTERNET`. See [`PRIVACY.md`](PRIVACY.md) and
[`docs/data-safety.md`](docs/data-safety.md).

## Known limitations (by design / platform)

- **Not biometric "Lockdown".** Android exposes no public API to trigger the
  power-menu Lockdown that disables fingerprint/face unlock. Panic Shield does a
  standard screen lock.
- **Lockscreen.** Accessibility services don't run on the lockscreen, so the
  volume trigger needs the screen on / the app reachable.
- **Background camera.** On Android 14+, the silent capture needs the "Display
  over other apps" exemption to start from the background.
- **App blocking** can be evaded via split-screen / picture-in-picture; it
  raises the bar, it isn't a sandbox.
- **OEM background killers** (Xiaomi/Huawei/etc.) can suspend the service —
  hence the battery-optimization exemption prompt.

## Project status

v1.1 feature-complete and verified on emulator. Pending: real-device + multi-OEM
testing, optional Lockdown power-menu automation, and Play Store assets/listing.
See [`docs/plan.md`](docs/plan.md) and [`docs/plan-v1.1.md`](docs/plan-v1.1.md).

---

*Android-only. No accounts, no tracking, no ads.*
