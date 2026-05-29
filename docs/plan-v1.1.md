# Panic Shield — v1.1 Expansion Plan

> Continues from `plan.md`. v1.0 shipped a screen-lock-only MVP whose value vs.
> the OS power button was thin. v1.1 adds the real differentiators: SOS SMS,
> silent face-detected camera capture, protected-app blocking, and an
> accessibility-driven Lockdown trigger.

**Goal:** Make Panic Shield genuinely useful in a robbery / coercion scenario by
adding three things the OS power button cannot do — alert a contact, capture
the attacker's face, and gate access to sensitive apps even after biometric
unlock.

**Architecture continuation:** Build on the existing AccessibilityService.
Introduce a `PanicState` flag (DataStore) flipped to ACTIVE when the trigger
fires, consumed by independent reactors (SmsReactor, CameraReactor,
BankBlocker). Reset via a "safe PIN" entered in the app.

**Permissions to add:** `SEND_SMS`, `ACCESS_FINE_LOCATION`, `CAMERA`,
`READ_CONTACTS`, `QUERY_ALL_PACKAGES`, `FOREGROUND_SERVICE` +
`FOREGROUND_SERVICE_LOCATION` + `FOREGROUND_SERVICE_CAMERA`.

**Time budget:** ~23h dev. At 3h/week → 7-8 weeks.

---

## File structure additions

```
app/src/main/java/com/intellica/panicshield/
├── panic/
│   ├── PanicState.kt              # enum: IDLE | ACTIVE
│   ├── PanicStateRepository.kt    # DataStore-backed flag
│   └── PanicCoordinator.kt        # observes state, fans out to reactors
├── sms/
│   ├── SosSmsReactor.kt           # location fetch + SmsManager
│   └── EmergencyContact.kt        # data class
├── camera/
│   ├── FaceCaptureService.kt      # ForegroundService, CameraX + ML Kit
│   └── CapturedPhoto.kt           # metadata
├── block/
│   ├── ProtectedAppsRepository.kt # DataStore set of package names
│   ├── BankBlocker.kt             # foreground-app monitor, fires HOME
│   └── KnownBanks.kt              # curated default list
├── lockdown/
│   └── LockdownHack.kt            # power-menu open + accessibility tap
└── ui/
    ├── contact/
    │   └── ContactPickerScreen.kt
    ├── protected/
    │   └── ProtectedAppsScreen.kt
    └── panic/
        └── SafeUnlockScreen.kt    # "panic active" UI with PIN entry
```

---

## Task 15: Panic state foundation

**Goal:** A single ACTIVE/IDLE flag everything else hangs off.

**Files:**
- Create: `panic/PanicState.kt`, `panic/PanicStateRepository.kt`
- Create: `panic/PanicCoordinator.kt`
- Modify: `service/PanicAccessibilityService.kt` (call PanicCoordinator instead of LockAction directly)

- [ ] **Step 1:** Write `PanicState` enum.

```kotlin
package com.intellica.panicshield.panic

enum class PanicState { IDLE, ACTIVE }
```

- [ ] **Step 2:** Write `PanicStateRepository` (DataStore extension).

```kotlin
package com.intellica.panicshield.panic

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.panicStore by preferencesDataStore(name = "panic_state")

class PanicStateRepository(private val context: Context) {
    val state: Flow<PanicState> = context.panicStore.data.map { prefs ->
        if (prefs[ACTIVE] == true) PanicState.ACTIVE else PanicState.IDLE
    }
    val activatedAt: Flow<Long?> = context.panicStore.data.map { it[ACTIVATED_AT] }

    suspend fun activate(now: Long) {
        context.panicStore.edit {
            it[ACTIVE] = true
            it[ACTIVATED_AT] = now
        }
    }
    suspend fun clear() {
        context.panicStore.edit {
            it[ACTIVE] = false
            it.remove(ACTIVATED_AT)
        }
    }

    private companion object {
        val ACTIVE = booleanPreferencesKey("active")
        val ACTIVATED_AT = longPreferencesKey("activated_at")
    }
}
```

- [ ] **Step 3:** `PanicCoordinator` — one entry point, fans out.

```kotlin
package com.intellica.panicshield.panic

import android.content.Context
import android.util.Log

class PanicCoordinator(private val context: Context) {
    fun fire() {
        Log.d("PanicCoord", "fire()")
        // v1.1 tasks plug in here:
        // - PanicStateRepository.activate
        // - LockAction.lockNow
        // - SosSmsReactor.start
        // - FaceCaptureService start intent
        // - BankBlocker re-arm
    }
}
```

(We'll flesh out `fire()` task-by-task as each reactor lands.)

- [ ] **Step 4:** Re-wire AccessibilityService to call `PanicCoordinator.fire()` instead of `buildLockAction().lockNow()` directly.

- [ ] **Step 5:** Commit.

```
feat(panic): introduce PanicState + PanicCoordinator scaffolding
```

---

## Task 16: SOS SMS reactor

**Goal:** Panic fires → fetch last known location → send SMS to chosen contact.

**Files:**
- Create: `sms/EmergencyContact.kt`, `sms/SosSmsReactor.kt`
- Create: `ui/contact/ContactPickerScreen.kt`
- Modify: `settings/SettingsRepository.kt` (add emergency contact storage)
- Modify: `AndroidManifest.xml` (SEND_SMS, ACCESS_FINE_LOCATION, FOREGROUND_SERVICE_LOCATION)
- Modify: `panic/PanicCoordinator.kt`

- [ ] **Step 1:** EmergencyContact data class.

```kotlin
data class EmergencyContact(val displayName: String, val phoneE164: String)
```

- [ ] **Step 2:** SettingsRepository — add contact storage. Key `emergency_contact_e164` (String) + `emergency_contact_name` (String). Flow: `emergencyContact: Flow<EmergencyContact?>`.

- [ ] **Step 3:** Manifest additions.

```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```

- [ ] **Step 4:** SosSmsReactor — a `ForegroundService` (location FG type). Flow:

```
1. start()
2. promoteToForeground(...) with type=location, persistent notif "Sending SOS..."
3. LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(PRIORITY_HIGH_ACCURACY, cts)
   - If <5s timeout → use lastLocation as fallback
4. Read EmergencyContact from settings
5. body = "Acil durumdayım. Konum: https://maps.google.com/?q=$lat,$lng (Panic Shield)"
6. SmsManager.sendTextMessage(phoneE164, null, body, sentPI, deliveredPI)
7. Log success/failure
8. stopSelf()
```

- [ ] **Step 5:** ContactPickerScreen — use ACTION_PICK intent on Contacts URI to let user pick. Save E.164 phone + display name. Normalize phone via `PhoneNumberUtils.formatNumberToE164(phone, "TR")`.

- [ ] **Step 6:** SettingsScreen — add "Emergency contact" row, tapping it launches ContactPickerScreen.

- [ ] **Step 7:** PanicCoordinator.fire() — start SmsReactor via `context.startForegroundService(Intent(...))`. Only if `EmergencyContact != null`.

- [ ] **Step 8:** Onboarding — add a 4th screen explaining SMS feature + tester to grant SEND_SMS + LOCATION at first use. Use `ActivityResultContracts.RequestMultiplePermissions`.

- [ ] **Step 9:** Manual test on emulator: `adb shell input keyevent 24` x3 via toolbar button, observe logcat for "SMS sent" + check AVD's outgoing SMS log (`adb logcat -s SmsManager:V`).

- [ ] **Step 10:** Commit.

```
feat(sms): SOS reactor with location + emergency contact picker
```

---

## Task 17: Face-detected silent camera capture

**Goal:** Panic fires → open front camera invisibly → detect face for ≤5s → save full-res frame to app-private storage.

**Files:**
- Create: `camera/CapturedPhoto.kt`, `camera/FaceCaptureService.kt`
- Modify: `gradle/libs.versions.toml` (CameraX + ML Kit face detection)
- Modify: `app/build.gradle.kts`
- Modify: `AndroidManifest.xml` (CAMERA, FOREGROUND_SERVICE_CAMERA)
- Modify: `panic/PanicCoordinator.kt`

- [ ] **Step 1:** libs.versions.toml additions.

```toml
[versions]
cameraX = "1.4.0"
mlkitFace = "16.1.7"

[libraries]
camerax-core = { group = "androidx.camera", name = "camera-core", version.ref = "cameraX" }
camerax-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "cameraX" }
camerax-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "cameraX" }
mlkit-face-detection = { group = "com.google.mlkit", name = "face-detection", version.ref = "mlkitFace" }
```

- [ ] **Step 2:** app/build.gradle.kts — add implementation lines.

- [ ] **Step 3:** Manifest additions.

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-feature android:name="android.hardware.camera.front" android:required="false" />
```

- [ ] **Step 4:** FaceCaptureService — `LifecycleService`, foreground type=camera. Flow:

```
1. promoteToForeground(camera type), persistent notif "Verifying surroundings..."
2. Build ImageAnalysis use case + ImageCapture use case
3. Bind to LifecycleOwner=this with CameraSelector.DEFAULT_FRONT_CAMERA
4. ImageAnalysis processes frames via ML Kit FaceDetector
5. On first frame with face confidence>0.7:
   - imageCapture.takePicture(...) into app-private dir
   - filename: panic_<timestamp>.jpg
6. After 5s OR successful capture → unbind + stopSelf
```

> Note: No PreviewView. CameraX can bind ImageAnalysis+ImageCapture without a
> Preview surface — capture is fully invisible.

- [ ] **Step 5:** CapturedPhoto data class (timestamp, file path) + a simple gallery view in Settings → "Captured photos" so user can review/delete.

- [ ] **Step 6:** PanicCoordinator.fire() — start FaceCaptureService.

- [ ] **Step 7:** Permission rationale UI — explain BEFORE requesting CAMERA permission. Play review needs this.

- [ ] **Step 8:** Manual test on AVD: trigger volume up x3, check `/data/data/com.intellica.panicshield/files/captures/` via `adb shell run-as` (AVD's front camera is simulated — usually a placeholder pattern, ML Kit may or may not detect "face" in it; if not, write fallback "no face detected" to logcat for confirmation).

- [ ] **Step 9:** Commit.

```
feat(camera): silent front-camera capture on face detection
```

---

## Task 18: Protected apps + BankBlocker

**Goal:** While panic ACTIVE, any foreground app in user's "protected" list is immediately closed via GLOBAL_ACTION_HOME.

**Files:**
- Create: `block/KnownBanks.kt`, `block/ProtectedAppsRepository.kt`, `block/BankBlocker.kt`
- Create: `ui/protected/ProtectedAppsScreen.kt`
- Modify: `service/PanicAccessibilityService.kt` (handle WINDOW_STATE_CHANGED, dispatch to BankBlocker)
- Modify: `AndroidManifest.xml` (QUERY_ALL_PACKAGES + queries fallback)

- [ ] **Step 1:** KnownBanks.kt — curated TR defaults.

```kotlin
object KnownBanks {
    val DEFAULT_TR = setOf(
        "com.ziraat.ziraatmobil",
        "com.akbank.android.apps.akbank_direkt",
        "com.garanti.cepsubesi",
        "com.isbank.mobile.bireysel",
        "com.ykb.androidtablet",
        "com.halkbank.mobiluygulama",
        "com.qnbfinansbank.cepsubesi",
        "com.ingbankasi.ingmobil",
        "com.denizbank.mobildeniz",
        "com.tebpos.tebmobil",
        "com.yapikredi.mobil",
        "com.papara.app",
        "com.ininal.wallet",
        "com.fastpay.app",
        "com.kuveytturk.androidnext",
    )
}
```

- [ ] **Step 2:** ProtectedAppsRepository — DataStore stringSetPreferencesKey "protected_packages". Default to intersection of `DEFAULT_TR` ∩ installed packages on first launch.

- [ ] **Step 3:** Manifest additions.

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
<!-- Required because user picks from arbitrary installed apps; Play justification: "Users select which sensitive apps Panic Shield should gate during a panic event." -->
```

- [ ] **Step 4:** ProtectedAppsScreen — list installed launcher-launchable apps (`PackageManager.queryIntentActivities(Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER), 0)`). Sort: protected on top, then alphabetic. Each row a checkbox.

- [ ] **Step 5:** BankBlocker — pure function:

```kotlin
class BankBlocker(
    private val protected: Set<String>,
    private val panicActive: Boolean,
) {
    fun shouldBlock(packageName: String?): Boolean =
        panicActive && packageName != null && packageName in protected
}
```

- [ ] **Step 6:** Unit tests for BankBlocker (3 cases: panic=false → never block, panic=true + protected → block, panic=true + unprotected → don't block).

- [ ] **Step 7:** PanicAccessibilityService — add `serviceInfo` runtime mutation to also subscribe `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`. In `onAccessibilityEvent`, read `event.packageName`, ask BankBlocker — if block → `performGlobalAction(GLOBAL_ACTION_HOME)`.

- [ ] **Step 8:** Manual test: enable protected, set panic ACTIVE via dev menu (we'll add a debug shortcut), open a "protected" app (use Chrome as stand-in by adding it to the list), verify it instantly gets kicked to home.

- [ ] **Step 9:** Commit.

```
feat(block): foreground-app blocker for protected packages during panic
```

---

## Task 19: Lockdown power-menu hack

**Goal:** When panic fires, programmatically open the system power menu and tap "Lockdown" — disabling biometrics for next unlock.

**Files:**
- Create: `lockdown/LockdownHack.kt`
- Modify: `service/PanicAccessibilityService.kt`
- Modify: `panic/PanicCoordinator.kt`
- Modify: `app/src/main/res/values/strings.xml` (Lockdown label heuristics)

- [ ] **Step 1:** LockdownHack — sequencer:

```kotlin
class LockdownHack(private val service: AccessibilityService) {
    private val labels = setOf("Lockdown", "Kilitleme", "Lock down")

    fun fire() {
        service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        // post a delayed search; the window-state event will arrive ~200-500ms
        service.serviceHandler?.postDelayed({ tryClickLockdown() }, 500)
    }

    private fun tryClickLockdown() {
        val root = service.rootInActiveWindow ?: return
        for (label in labels) {
            root.findAccessibilityNodeInfosByText(label)
                .firstOrNull()
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ?.let { if (it) return }
        }
        // fallback: dismiss the menu so we don't leave it open
        service.performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
```

- [ ] **Step 2:** SettingsScreen — add a one-time "Verify Lockdown availability" diagnostic button that runs the hack and reports success/failure. Also include text: "Make sure Settings → Display → Lock screen → 'Show lockdown option' is enabled."

- [ ] **Step 3:** Onboarding — page about Lockdown setup + deep link to `Settings.ACTION_DISPLAY_SETTINGS`.

- [ ] **Step 4:** PanicCoordinator.fire() — call LockdownHack BEFORE LockAction (Lockdown does its own screen lock). Add a settings toggle to disable if it misbehaves on a specific OEM.

- [ ] **Step 5:** Manual test: on Pixel emulator with "Show lockdown option" enabled, fire panic → power menu opens → Lockdown gets tapped → screen locks with biometric disabled (visible: only PIN entry, no fingerprint option).

- [ ] **Step 6:** Commit.

```
feat(lockdown): power-menu accessibility hack to disable biometrics
```

---

## Task 20: Safe-unlock UI + panic lifecycle

**Goal:** While panic is ACTIVE, app's home screen replaces its normal UI with a "Panic state — enter your safe PIN to disarm" screen. This is the only way to clear panic state and re-allow protected apps.

**Files:**
- Create: `ui/panic/SafeUnlockScreen.kt`
- Modify: `MainActivity.kt` (stage machine — add PanicActive stage)
- Modify: `settings/SettingsRepository.kt` (safe PIN storage, hashed)

- [ ] **Step 1:** Safe PIN setup — onboarding step: user picks a 4-6 digit safe PIN. Stored as `SecretKeySpec`-derived hash (PBKDF2, 100k iterations) in DataStore. Not the device PIN; different on purpose.

- [ ] **Step 2:** SafeUnlockScreen — keypad UI, on correct PIN entry → call `PanicStateRepository.clear()` and route to Home.

- [ ] **Step 3:** MainActivity Stage enum gets new value `PanicActive`. Observed via PanicStateRepository.state Flow. When state==ACTIVE → always show SafeUnlockScreen regardless of other state.

- [ ] **Step 4:** Auto-clear timeout — settings option, default OFF: if panic active for N hours, auto-clear. Off by default because panic should stay active until user explicitly disarms.

- [ ] **Step 5:** Commit.

```
feat(panic): safe-PIN unlock screen + panic lifecycle
```

---

## Task 21: Privacy policy + Data Safety updates

**Goal:** Document v1.1's new permission usage transparently. Play review will scrutinize SEND_SMS, CAMERA, QUERY_ALL_PACKAGES, FOREGROUND_SERVICE_*.

**Files:**
- Modify: `PRIVACY.md`
- Modify: `docs/data-safety.md`
- Modify: `docs/qa-checklist.md` (add v1.1 test cases)

- [ ] **Step 1:** Update PRIVACY.md — declare what's collected (photos: device-only, never uploaded; SMS: sent on user trigger to user-picked contact; location: read once on trigger, included in SMS, never stored).

- [ ] **Step 2:** Update Data Safety form — list "User data: Photos" (collected, device-only, not shared); "User data: Location" (collected, included in SMS to user's chosen contact, not stored by us); "Personal info: Phone numbers" (the emergency contact).

- [ ] **Step 3:** Add QA cases for SMS, camera, bank block, safe PIN.

- [ ] **Step 4:** Commit.

```
docs: update privacy + data safety for v1.1 features
```

---

## Risk additions

| # | Risk | Mitigation |
|---|---|---|
| R13 | Play review rejects QUERY_ALL_PACKAGES | Use `<queries>` for known banks, request QUERY_ALL_PACKAGES only behind a settings toggle "Add custom protected apps" |
| R14 | Play review rejects CAMERA usage as "covert recording" | Onboarding screen explicitly explains capture is trigger-only, photos stored device-only, never shared. User can view/delete in app. |
| R15 | SEND_SMS abuse vector | Hardcoded message template, single recipient, throttle 1 SMS per panic event |
| R16 | LockdownHack flaky on OEMs | Settings diagnostic button + opt-out toggle + Closed Test OEM matrix in `docs/qa-checklist.md` |
| R17 | BankBlocker bypassed via split-screen / picture-in-picture | Accept the limit; document. Recents-list + relaunch attacks rate-limited by HOME action |
| R18 | Safe PIN forgotten → user locked out of own banks | Settings → "Reset safe PIN" requires device biometric/PIN (Android KeyguardManager) as fallback escape hatch |

## Definition of Done (v1.1)

- [ ] All Tasks 15-21 complete + manual QA passes
- [ ] On Pixel emulator + 1 real device: trigger → SMS sent + photo captured + Lockdown engaged + Chrome (mock-bank) blocked while panic active + safe PIN clears state
- [ ] Play Closed Test build accepted with QUERY_ALL_PACKAGES + SEND_SMS + CAMERA justifications
