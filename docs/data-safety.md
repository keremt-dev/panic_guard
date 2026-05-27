# Play Data Safety — Panic Shield

Values to enter in the Play Console Data Safety form.

## Data collection & sharing
- Does your app collect or share any of the required user data types? **No**
- Is all user data encrypted in transit? **N/A** (no data leaves device)
- Do you provide a way for users to request that their data be deleted? **N/A** (no data collected)

## Permission justifications

### Accessibility Service
"Panic Shield's sole feature is detecting three rapid Volume Up hardware key presses and invoking the system lock-screen global action. The Accessibility Service is used solely to receive `KEYCODE_VOLUME_UP` events via `flagRequestFilterKeyEvents` and to call `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`. The service does not read on-screen content, does not capture window contents, and does not perform any gestures. This is the only Android API that exposes the volume-key event when the app is in the background, which is required for the panic trigger to be useful."

### Vibrate
"Brief 150ms haptic confirmation when the panic trigger fires. User-toggleable."

### Request Ignore Battery Optimizations
"The Accessibility Service must remain ready to receive key events at any time. Some OEMs aggressively suspend non-exempt apps, which would silently disable the panic trigger. The exemption request informs the user of this and lets them opt in."
