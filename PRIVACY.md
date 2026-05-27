# Panic Shield — Privacy Policy

**Last updated:** 2026-05-27

Panic Shield is an Android application whose only function is to detect three rapid presses of the Volume Up hardware key and immediately lock the device screen.

## Data we collect

**None.** Panic Shield does not collect, store, or transmit any user data. The app contains no analytics SDK, no crash reporter, no advertising library, and has no `INTERNET` permission.

## Permissions

- **Accessibility Service (BIND_ACCESSIBILITY_SERVICE):** required to receive Volume Up key events while the app is in the background and to invoke the system "lock screen" global action. Panic Shield does not read on-screen content. The service uses `flagRequestFilterKeyEvents` exclusively and only reacts to `KEYCODE_VOLUME_UP`.
- **VIBRATE:** to buzz briefly when the trigger fires (toggleable in settings).
- **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:** to ask the user to exempt Panic Shield from battery optimization so the service remains reliable.

## Children

Panic Shield is suitable for all ages but is not directed at children under 13. The app collects no information from anyone.

## Changes

If the privacy posture changes (which would only happen if a future opt-in feature requires it), this document will be updated and the in-app onboarding will surface the change before the new feature can be enabled.

## Contact

kerem.turkyilmaz@intellica.net
