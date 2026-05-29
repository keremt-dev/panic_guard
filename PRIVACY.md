# Panic Shield — Privacy Policy

**Last updated:** 2026-05-29

Panic Shield is an Android personal-safety app. When you press the Volume Up
key three times in quick succession ("panic trigger"), it locks your screen and
runs the safeguards you have enabled: alerting an emergency contact, capturing a
photo of whoever next wakes/unlocks the phone, and blocking the apps you marked
as protected until you disarm with your safe PIN.

## Summary

- **No accounts. No backend. No internet.** Panic Shield has **no `INTERNET`
  permission** — it cannot and does not upload anything to us or to any server.
- **We do not collect, receive, or have access to any of your data.** Everything
  the app stores stays on your device.
- The only data that ever leaves your device is an **SMS you trigger yourself**,
  sent through your carrier to the contact **you** chose.

## What the app handles, and where it stays

| Data | When | Where it goes |
|---|---|---|
| **Photos** (front camera) | Captured when the phone is woken/unlocked while panic is active | Saved only in the app's private internal storage. Never uploaded. You can view and delete them in-app. Removed when you uninstall. |
| **Location** | Read once at panic time, only if you set an emergency contact and granted location permission | Inserted as a Google Maps link into the SOS SMS to your chosen contact. Not stored by the app, not sent to us. |
| **Emergency contact** (name + phone number) | When you pick it in Settings | Stored only on your device. Used solely to address the SOS SMS. |
| **SOS SMS** | Sent at panic time, if a contact is set | Sent via your device/carrier to your chosen contact. Fixed message + location link. Not routed through us. |
| **Safe PIN** | When you set it | Stored only as a salted PBKDF2 hash on your device. Never transmitted. Not recoverable by us. |
| **Protected apps list** | When you choose apps | A list of app package names, stored only on your device. |
| **Trigger settings** | When you change them | Stored only on your device. |

There is **no analytics SDK, no crash reporter, and no advertising** in Panic
Shield.

## Permissions and why they are needed

- **Accessibility Service (BIND_ACCESSIBILITY_SERVICE):** to detect the Volume Up
  hardware-key trigger while the app is in the background, to lock the screen,
  and — while panic is active — to send a protected app back to the home screen
  when it is opened. It only reacts to the volume key and the foreground app's
  package name; it does not read or transmit on-screen content.
- **CAMERA + FOREGROUND_SERVICE_CAMERA:** to take the silent safety photo when
  the phone is woken/unlocked during panic. Photos stay on the device.
- **SEND_SMS:** to send the SOS message to your chosen contact at panic time.
- **ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION + FOREGROUND_SERVICE_LOCATION:**
  to include your location in the SOS SMS. Read only at panic time; not stored.
- **READ_CONTACTS:** only used by the system contact picker so you can choose an
  emergency contact. The app does not read your contact list otherwise.
- **QUERY_ALL_PACKAGES:** so you can pick which installed apps to protect during
  a panic event. The list of apps is shown only to you and never leaves the
  device.
- **SYSTEM_ALERT_WINDOW ("Display over other apps"):** grants the background
  exemption needed to start the safety-photo capture when the phone is woken
  while locked.
- **POST_NOTIFICATIONS:** to show the brief foreground-service notification
  while the SOS/photo task runs (required by Android).
- **VIBRATE:** brief haptic confirmation when the trigger fires (toggleable).
- **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:** to keep the trigger reliable on
  devices that aggressively suspend background apps.

## Data sharing

We do not share your data with anyone, because we never receive it. The SOS SMS
is sent by your device to the recipient **you** selected; its delivery is
handled by your mobile carrier under their terms.

## Data deletion

All app data is stored locally. Uninstalling Panic Shield deletes it, including
captured photos, your safe PIN hash, protected-apps list, and emergency contact.
You can also delete captured photos individually in the app.

## Children

Panic Shield is not directed at children under 13 and collects no information
from anyone.

## Changes

If a future version changes how data is handled, this policy will be updated and
the in-app onboarding will explain the change before any new behavior is enabled.

## Contact

kerem.turkyilmaz@intellica.net
