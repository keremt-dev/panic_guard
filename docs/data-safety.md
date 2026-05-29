# Play Data Safety + Permissions Declarations — Panic Shield

Source-of-truth for the Play Console **Data Safety** form and the separate
**sensitive-permission declarations**. Filling these wrong can get the app
removed, so read the rationale, don't just copy blindly.

---

## 1. Data Safety form

### Key fact
Panic Shield has **no `INTERNET` permission** and **no backend**. Nothing is
transmitted to the developer or any server. All data is stored on-device.

### Per data type

| Data type | On device | Leaves device? | Declare as collected/shared? |
|---|---|---|---|
| Photos (camera) | App-private storage only | No | **No** — on-device only |
| Precise location | Read at panic time | Yes — inside the SOS SMS to the user's chosen contact | See note below |
| Phone number (emergency contact) | Local prefs | No (it's the SMS *recipient*) | **No** — on-device only |
| Safe PIN | Local, salted PBKDF2 hash | No | **No** |
| Installed-apps list | Local prefs | No | **No** |

### Recommended answers
- **Does your app collect or share any required user data types?**
  Recommended: **No**, relying on Google's exemptions:
  - On-device-only data (photos, contact, PIN, app list) is **not "collection"**
    under Play's definition (collection = transmitted off-device).
  - The location in the SOS SMS qualifies for the **user-initiated action
    exemption**: it is sent only after the user's explicit panic trigger, to a
    recipient the user chose, who the user reasonably expects to receive it.
    Google's policy states such data "does not need to be disclosed."
- **Encrypted in transit?** N/A for the form if declaring no collection. (The
  SMS itself is carrier-handled.)
- **Way to request deletion?** All data is local; uninstall deletes everything,
  and photos are individually deletable in-app.

> **If a reviewer pushes back on location:** switch to declaring **Location →
> collected = No, shared = Yes**, purpose **App functionality**, mark it
> **user-initiated / ephemeral, not for tracking, not sold**. Keep the
> justification text from §2 ready.

---

## 2. Sensitive permission declarations (separate Console sections)

These are scrutinized individually and may require a **demo video** of the
feature in use.

### Accessibility Service (BIND_ACCESSIBILITY_SERVICE)
"Panic Shield is a personal-safety app. The Accessibility Service is used to (1)
detect a triple Volume-Up hardware-key press while backgrounded and lock the
screen, and (2) while a panic is active, return a user-selected 'protected' app
to the home screen when it is opened. It reads only the volume key and the
foreground package name. It does not read, log, or transmit on-screen content,
and performs no gestures. Background key-event access is the only Android API
that makes the panic trigger usable when the app is not in the foreground."

### SEND_SMS
"Used solely to send a single user-configured SOS text message to one
emergency contact the user selected, triggered by the user's own panic gesture.
The message is a fixed safety template plus a location link. No bulk, premium,
marketing, or automated messaging. SMS is sent at most once per panic event."

> Note: SMS is a restricted permission. Panic Shield's use (manual SOS/personal
> safety) is a permitted core-functionality case, but the listing must clearly
> present SOS-SMS as a core feature and the in-app flow must let the user pick
> the contact. A demo video showing trigger → SOS SMS is expected.

### Location (ACCESS_FINE_LOCATION + FOREGROUND_SERVICE_LOCATION)
"Location is read only at the moment of a user-triggered panic, and only to
include a map link in the SOS SMS to the user's chosen contact. It is not stored
and not sent to the developer. Foreground-service-location type is used because
the fix is taken while the screen may be off after the panic lock."

### QUERY_ALL_PACKAGES
"Required so the user can choose, from a list of their installed apps, which apps
Panic Shield should block while a panic is active. The list is presented only to
the user and never leaves the device. No alternative (`<queries>`) suffices
because the user may protect any arbitrary app, not a known fixed set."

### Camera + FOREGROUND_SERVICE_CAMERA
"Used to take a single still photo from the front camera when the phone is woken
or unlocked while a panic is active, to help identify who has the device. Photos
are stored only in app-private storage and are never uploaded. The user can
disable this feature and can view/delete photos in-app."

### SYSTEM_ALERT_WINDOW
"Held to obtain the background foreground-service-start exemption needed to begin
the safety-photo capture when the phone is woken while locked. Panic Shield does
not draw persistent overlays."

---

## 3. Listing / review checklist
- [ ] Store listing presents SOS SMS, safety photo, app-blocking, and panic-lock
      as core safety features (justifies the permissions).
- [ ] Privacy policy URL points to the hosted PRIVACY.md.
- [ ] Demo video: trigger → lock → unlock → photo + SMS + blocked app → disarm.
- [ ] Accessibility, SMS, Location, Camera declarations filled per §2.
- [ ] Data Safety filled per §1.
