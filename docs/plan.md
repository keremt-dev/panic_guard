# Panic Shield — MVP v1.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship an Android app whose only job is "Volume up x3 → instantly lock the screen," ride-shared into Google Play production via the same onboarding playbook as ScanQuiet.

**Architecture:** Native Kotlin + Jetpack Compose. One `AccessibilityService` filters key events with `flagRequestFilterKeyEvents` and feeds a pure `PressTracker` (sliding 2-second window). On trigger, the service calls `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`. Settings persist in `DataStore<Preferences>`. No backend, no network, no data collection.

**Tech Stack:** Kotlin 2.0+, AGP 8.5+, Jetpack Compose (BOM 2024.10+), Accessibility Service API, DataStore Preferences, JUnit4 + Truth + Robolectric (only where needed), MockK.

**Time budget:** 3h/week × ~10 weeks. Out-of-budget items: Play Store tester recruiting, identity verification, asset creation (mirror ScanQuiet's §6 playbook).

---

## 0. Locked-in spec (single page)

### 0.1 Identity
- **Working name:** `Panic Shield` (placeholder)
- **Package:** `com.intellica.panicshield`
- **Tagline (EN):** "Volume up three times. Lock now."
- **Category:** Tools

### 0.2 Honest USP & known limits
- **What it does:** Three rapid `VOLUME_UP` presses (default: 3 presses within 2.0 seconds) trigger an immediate screen lock via Android's Accessibility Service global action.
- **What it does NOT do (be honest in listing):**
  - It does **not** disable biometrics. Android's true "Lockdown mode" (the one in the power menu that requires PIN instead of fingerprint) has **no public API**. Users wanting biometric-disabled state must enable "Show lockdown option" in system settings and use the power menu manually.
  - It does **not** wipe data, hide apps, or send SMS in v1.0.
  - It does **not** work without Accessibility Service permission granted by the user.

### 0.3 Scope

| # | Feature | Hours |
|---|---|---|
| F1 | Accessibility Service that filters volume key events | 3 |
| F2 | PressTracker pure-logic sliding window | 2 |
| F3 | Lock action + haptic feedback | 1 |
| F4 | Onboarding: accessibility-permission flow with deep-link to settings | 3 |
| F5 | Home screen: status (active/inactive), test button | 3 |
| F6 | Settings: enable/disable, press count (2–5), window (1.0–4.0s), vibration toggle | 3 |
| F7 | Battery optimization exemption request (Android 6+) | 1 |
| F8 | App icon + theme + branding | 2 |
| F9 | Privacy policy + Play Data Safety form data | 2 |
| F10 | Manual E2E checklist runs | 2 |

**Total dev:** ~22h. With 3h/week → ~8 coding weeks + 2 store-onboarding weeks.

**Out of scope for v1.0** (revisit after launch):
- Shake/voice/wear-os triggers
- Banking-app hiding (work profile pause)
- SOS SMS
- Silent photo / location ping
- Wear OS companion
- iOS — Android-only forever (iOS has no Accessibility-Service-equivalent for key events).

### 0.4 Permissions
- `BIND_ACCESSIBILITY_SERVICE` (granted via system settings, not runtime)
- `VIBRATE` (normal, no prompt)
- `POST_NOTIFICATIONS` (Android 13+, optional — only for "service running" persistent notification if we add one in v1.1; not in v1.0)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (request via intent)

No `INTERNET` permission. App is fully offline.

### 0.5 Data
- No network. No analytics. No crash reporting in v1.0 (Sentry/PostHog optional in v1.1 if needed; add `INTERNET` only then).
- DataStore stores `{enabled: Bool, pressCount: Int, windowMs: Long, vibrate: Bool}`. That's it.

### 0.6 Monetization
- **Free, no ads, no IAP for v1.0.** Goal of v1.0 is to learn Play onboarding for the second time (after ScanQuiet) and own a clean utility listing. v1.1 may add "Premium triggers" (shake, voice, wear) if v1.0 gets traction.

### 0.7 Definition of Done
- [ ] Closed Test → Production live
- [ ] Crash-free ≥99% over first 14 days
- [ ] Volume-up trigger works on at least 3 OEMs (Pixel, Samsung, Xiaomi) in tester reports
- [ ] At least 1 review or rating ≥4★

---

## 1. Repo layout

A **separate repository** (not in this monorepo). Created in Phase 1.

```
panic-shield/                                    # new git repo
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/intellica/panicshield/
│       │   │   ├── PanicApp.kt                   # Application class
│       │   │   ├── MainActivity.kt
│       │   │   ├── action/
│       │   │   │   └── LockAction.kt             # interface, fake for tests
│       │   │   ├── service/
│       │   │   │   ├── PanicAccessibilityService.kt
│       │   │   │   └── PressTracker.kt           # pure logic
│       │   │   ├── settings/
│       │   │   │   ├── SettingsRepository.kt     # DataStore
│       │   │   │   └── TriggerConfig.kt
│       │   │   └── ui/
│       │   │       ├── theme/
│       │   │       │   ├── Color.kt
│       │   │       │   ├── Theme.kt
│       │   │       │   └── Type.kt
│       │   │       ├── home/HomeScreen.kt
│       │   │       ├── onboarding/OnboardingScreen.kt
│       │   │       └── settings/SettingsScreen.kt
│       │   └── res/
│       │       ├── xml/accessibility_service_config.xml
│       │       ├── values/strings.xml
│       │       └── ...
│       └── test/
│           └── java/com/intellica/panicshield/
│               ├── service/PressTrackerTest.kt
│               └── settings/TriggerConfigTest.kt
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml                       # version catalog
├── .gitignore
├── README.md
└── PRIVACY.md
```

---

## 2. Tasks

### Task 1: Bootstrap new repo

**Files:**
- Create: `panic-shield/` directory **outside this monorepo**, suggested at `C:\kt\panic-shield`
- Create: `panic-shield/README.md`
- Create: `panic-shield/.gitignore`
- Move: `docs/superpowers/plans/2026-05-27-panic-shield-mvp.md` → `panic-shield/docs/plan.md`

- [ ] **Step 1: Create repo directory and git init**

```powershell
New-Item -ItemType Directory -Path C:\kt\panic-shield
cd C:\kt\panic-shield
git init
```

- [ ] **Step 2: Write minimal README**

`panic-shield/README.md`:
```markdown
# Panic Shield

Volume up three times. Lock now.

Android-only utility. No network, no accounts, no tracking.

See `docs/plan.md`.
```

- [ ] **Step 3: Write Android-flavored .gitignore**

`panic-shield/.gitignore`:
```
# Built application files
*.apk
*.aar
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
release/

# Gradle files
.gradle/
build/

# Local configuration file (sdk path, etc)
local.properties

# Proguard folder generated by Eclipse
proguard/

# Log Files
*.log

# Android Studio Navigation editor temp files
.navigation/

# Android Studio captures folder
captures/

# IntelliJ
*.iml
.idea/
```

- [ ] **Step 4: Copy this plan into the new repo**

```powershell
New-Item -ItemType Directory -Path C:\kt\panic-shield\docs
Copy-Item C:\kt\upwork\app_store\.claude\worktrees\nostalgic-goldwasser-ba70c6\docs\superpowers\plans\2026-05-27-panic-shield-mvp.md C:\kt\panic-shield\docs\plan.md
```

- [ ] **Step 5: First commit**

```powershell
cd C:\kt\panic-shield
git add .
git commit -m "chore: bootstrap panic-shield repo"
```

---

### Task 2: Create Android Studio project skeleton

**Files:**
- Create: `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/intellica/panicshield/MainActivity.kt`

**Approach:** Use Android Studio's "Empty Activity (Compose)" template, then modify. If working headless, write the files directly per below.

- [ ] **Step 1: Open Android Studio → New Project → Empty Activity (Compose)**

Settings:
- Name: `Panic Shield`
- Package: `com.intellica.panicshield`
- Save location: `C:\kt\panic-shield`
- Minimum SDK: **API 28 (Android 9.0)** — required for `GLOBAL_ACTION_LOCK_SCREEN`
- Build configuration language: Kotlin DSL (.kts)

(If Android Studio creates files in a subdir like `PanicShield/`, move them up to the repo root.)

- [ ] **Step 2: Set up version catalog**

`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.20"
compose-bom = "2024.10.00"
core-ktx = "1.13.1"
lifecycle = "2.8.6"
activity-compose = "1.9.2"
datastore = "1.1.1"
junit = "4.13.2"
truth = "1.4.4"
mockk = "1.13.12"
robolectric = "4.13"

[libraries]
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 3: Configure `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.intellica.panicshield"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.intellica.panicshield"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}
```

- [ ] **Step 4: Verify build works**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. If on Windows: `.\gradlew.bat assembleDebug`.

- [ ] **Step 5: Commit**

```powershell
git add .
git commit -m "chore: scaffold Android project (Kotlin + Compose, minSdk 28)"
```

---

### Task 3: PressTracker (pure logic, TDD)

**Files:**
- Create: `app/src/main/java/com/intellica/panicshield/service/PressTracker.kt`
- Create: `app/src/test/java/com/intellica/panicshield/service/PressTrackerTest.kt`

The tracker keeps a deque of recent press timestamps (ms). On every press, it drops timestamps older than `windowMs`. If remaining count ≥ `requiredPresses`, it fires.

- [ ] **Step 1: Write failing test**

`app/src/test/java/com/intellica/panicshield/service/PressTrackerTest.kt`:
```kotlin
package com.intellica.panicshield.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PressTrackerTest {

    @Test
    fun `three presses within window fires`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        assertThat(tracker.record(timeMs = 0L)).isFalse()
        assertThat(tracker.record(timeMs = 500L)).isFalse()
        assertThat(tracker.record(timeMs = 1000L)).isTrue()
    }

    @Test
    fun `three presses outside window does not fire`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        tracker.record(timeMs = 0L)
        tracker.record(timeMs = 1500L)
        assertThat(tracker.record(timeMs = 3000L)).isFalse()
    }

    @Test
    fun `presses outside window expire and do not count`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        tracker.record(timeMs = 0L)         // expires by 2500
        tracker.record(timeMs = 2200L)
        tracker.record(timeMs = 2400L)
        assertThat(tracker.record(timeMs = 2600L)).isTrue()
    }

    @Test
    fun `after firing tracker resets so next trigger needs full sequence`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        tracker.record(timeMs = 0L)
        tracker.record(timeMs = 500L)
        tracker.record(timeMs = 1000L) // fires
        assertThat(tracker.record(timeMs = 1100L)).isFalse()
        assertThat(tracker.record(timeMs = 1200L)).isFalse()
        assertThat(tracker.record(timeMs = 1300L)).isTrue()
    }

    @Test
    fun `count of 2 within window fires`() {
        val tracker = PressTracker(requiredPresses = 2, windowMs = 1000)
        assertThat(tracker.record(timeMs = 0L)).isFalse()
        assertThat(tracker.record(timeMs = 500L)).isTrue()
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.intellica.panicshield.service.PressTrackerTest"`
Expected: FAIL — `Unresolved reference: PressTracker`

- [ ] **Step 3: Implement `PressTracker`**

`app/src/main/java/com/intellica/panicshield/service/PressTracker.kt`:
```kotlin
package com.intellica.panicshield.service

import java.util.ArrayDeque

class PressTracker(
    private val requiredPresses: Int,
    private val windowMs: Long,
) {
    private val timestamps = ArrayDeque<Long>()

    fun record(timeMs: Long): Boolean {
        timestamps.addLast(timeMs)
        while (timestamps.isNotEmpty() && timeMs - timestamps.peekFirst() > windowMs) {
            timestamps.removeFirst()
        }
        return if (timestamps.size >= requiredPresses) {
            timestamps.clear()
            true
        } else {
            false
        }
    }
}
```

- [ ] **Step 4: Run tests, confirm they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.intellica.panicshield.service.PressTrackerTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/intellica/panicshield/service/PressTracker.kt app/src/test/java/com/intellica/panicshield/service/PressTrackerTest.kt
git commit -m "feat(service): PressTracker sliding-window trigger logic"
```

---

### Task 4: TriggerConfig + SettingsRepository (DataStore)

**Files:**
- Create: `app/src/main/java/com/intellica/panicshield/settings/TriggerConfig.kt`
- Create: `app/src/main/java/com/intellica/panicshield/settings/SettingsRepository.kt`
- Create: `app/src/test/java/com/intellica/panicshield/settings/TriggerConfigTest.kt`

- [ ] **Step 1: Write failing test for defaults and validation**

`app/src/test/java/com/intellica/panicshield/settings/TriggerConfigTest.kt`:
```kotlin
package com.intellica.panicshield.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TriggerConfigTest {

    @Test
    fun `default config matches MVP spec`() {
        val config = TriggerConfig.DEFAULT
        assertThat(config.enabled).isTrue()
        assertThat(config.pressCount).isEqualTo(3)
        assertThat(config.windowMs).isEqualTo(2000L)
        assertThat(config.vibrate).isTrue()
    }

    @Test
    fun `pressCount is clamped to allowed range`() {
        assertThat(TriggerConfig.DEFAULT.copy(pressCount = 1).normalized().pressCount).isEqualTo(2)
        assertThat(TriggerConfig.DEFAULT.copy(pressCount = 99).normalized().pressCount).isEqualTo(5)
    }

    @Test
    fun `windowMs is clamped to allowed range`() {
        assertThat(TriggerConfig.DEFAULT.copy(windowMs = 100L).normalized().windowMs).isEqualTo(1000L)
        assertThat(TriggerConfig.DEFAULT.copy(windowMs = 99_999L).normalized().windowMs).isEqualTo(4000L)
    }
}
```

- [ ] **Step 2: Run test, confirm failure**

Run: `./gradlew :app:testDebugUnitTest --tests "com.intellica.panicshield.settings.TriggerConfigTest"`
Expected: FAIL — `Unresolved reference: TriggerConfig`

- [ ] **Step 3: Implement `TriggerConfig`**

`app/src/main/java/com/intellica/panicshield/settings/TriggerConfig.kt`:
```kotlin
package com.intellica.panicshield.settings

data class TriggerConfig(
    val enabled: Boolean,
    val pressCount: Int,
    val windowMs: Long,
    val vibrate: Boolean,
) {
    fun normalized(): TriggerConfig = copy(
        pressCount = pressCount.coerceIn(MIN_PRESS_COUNT, MAX_PRESS_COUNT),
        windowMs = windowMs.coerceIn(MIN_WINDOW_MS, MAX_WINDOW_MS),
    )

    companion object {
        const val MIN_PRESS_COUNT = 2
        const val MAX_PRESS_COUNT = 5
        const val MIN_WINDOW_MS = 1000L
        const val MAX_WINDOW_MS = 4000L

        val DEFAULT = TriggerConfig(
            enabled = true,
            pressCount = 3,
            windowMs = 2000L,
            vibrate = true,
        )
    }
}
```

- [ ] **Step 4: Run tests, confirm pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.intellica.panicshield.settings.TriggerConfigTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Implement `SettingsRepository` (no test — DataStore is framework, exercised in integration)**

`app/src/main/java/com/intellica/panicshield/settings/SettingsRepository.kt`:
```kotlin
package com.intellica.panicshield.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "panic_shield_settings")

class SettingsRepository(private val context: Context) {

    private val ENABLED = booleanPreferencesKey("enabled")
    private val PRESS_COUNT = intPreferencesKey("press_count")
    private val WINDOW_MS = longPreferencesKey("window_ms")
    private val VIBRATE = booleanPreferencesKey("vibrate")

    val config: Flow<TriggerConfig> = context.dataStore.data.map { prefs ->
        TriggerConfig(
            enabled = prefs[ENABLED] ?: TriggerConfig.DEFAULT.enabled,
            pressCount = prefs[PRESS_COUNT] ?: TriggerConfig.DEFAULT.pressCount,
            windowMs = prefs[WINDOW_MS] ?: TriggerConfig.DEFAULT.windowMs,
            vibrate = prefs[VIBRATE] ?: TriggerConfig.DEFAULT.vibrate,
        ).normalized()
    }

    suspend fun update(transform: (TriggerConfig) -> TriggerConfig) {
        context.dataStore.edit { prefs ->
            val current = TriggerConfig(
                enabled = prefs[ENABLED] ?: TriggerConfig.DEFAULT.enabled,
                pressCount = prefs[PRESS_COUNT] ?: TriggerConfig.DEFAULT.pressCount,
                windowMs = prefs[WINDOW_MS] ?: TriggerConfig.DEFAULT.windowMs,
                vibrate = prefs[VIBRATE] ?: TriggerConfig.DEFAULT.vibrate,
            )
            val next = transform(current).normalized()
            prefs[ENABLED] = next.enabled
            prefs[PRESS_COUNT] = next.pressCount
            prefs[WINDOW_MS] = next.windowMs
            prefs[VIBRATE] = next.vibrate
        }
    }
}
```

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/intellica/panicshield/settings/ app/src/test/java/com/intellica/panicshield/settings/
git commit -m "feat(settings): TriggerConfig + DataStore repository"
```

---

### Task 5: LockAction abstraction

**Files:**
- Create: `app/src/main/java/com/intellica/panicshield/action/LockAction.kt`

Why an interface: keeps the AccessibilityService thin and lets us swap it in instrumentation tests later. Also documents the single OS dependency.

- [ ] **Step 1: Implement**

`app/src/main/java/com/intellica/panicshield/action/LockAction.kt`:
```kotlin
package com.intellica.panicshield.action

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context

interface LockAction {
    fun lockNow()
}

class AccessibilityLockAction(
    private val service: AccessibilityService,
    private val vibrate: Boolean,
) : LockAction {

    override fun lockNow() {
        if (vibrate) buzz()
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    private fun buzz() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = service.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
```

- [ ] **Step 2: Commit**

```powershell
git add app/src/main/java/com/intellica/panicshield/action/LockAction.kt
git commit -m "feat(action): LockAction interface + accessibility-backed impl"
```

---

### Task 6: AccessibilityService + manifest wiring

**Files:**
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Create: `app/src/main/java/com/intellica/panicshield/service/PanicAccessibilityService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write accessibility service config**

`app/src/main/res/xml/accessibility_service_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFlags="flagRequestFilterKeyEvents|flagDefault"
    android:canRequestFilterKeyEvents="true"
    android:canPerformGestures="false"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:summary="@string/accessibility_service_summary" />
```

- [ ] **Step 2: Add the two required strings**

In `app/src/main/res/values/strings.xml` (create if missing — append if exists):
```xml
<resources>
    <string name="app_name">Panic Shield</string>
    <string name="accessibility_service_description">Detects three rapid volume-up presses and locks the screen. Panic Shield does not read screen content; it only listens for the volume-up hardware key.</string>
    <string name="accessibility_service_summary">Volume up x3 → instant lock</string>
</resources>
```

- [ ] **Step 3: Implement the service**

`app/src/main/java/com/intellica/panicshield/service/PanicAccessibilityService.kt`:
```kotlin
package com.intellica.panicshield.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.intellica.panicshield.action.AccessibilityLockAction
import com.intellica.panicshield.action.LockAction
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.settings.TriggerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Dispatchers

class PanicAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settings: SettingsRepository
    private var tracker: PressTracker = PressTracker(
        TriggerConfig.DEFAULT.pressCount,
        TriggerConfig.DEFAULT.windowMs,
    )
    private var currentConfig: TriggerConfig = TriggerConfig.DEFAULT

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(applicationContext)
        settings.config
            .onEach { config ->
                currentConfig = config
                tracker = PressTracker(config.pressCount, config.windowMs)
            }
            .launchIn(scope)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!currentConfig.enabled) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.repeatCount != 0) return false

        if (tracker.record(event.eventTime)) {
            buildLockAction().lockNow()
        }
        return false  // do NOT consume — let volume change normally
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildLockAction(): LockAction =
        AccessibilityLockAction(service = this, vibrate = currentConfig.vibrate)
}
```

- [ ] **Step 4: Wire the service in AndroidManifest.xml**

`app/src/main/AndroidManifest.xml` (overwrite full file):
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:name=".PanicApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.PanicShield"
        tools:targetApi="31"
        xmlns:tools="http://schemas.android.com/tools">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.PanicShield">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.PanicAccessibilityService"
            android:exported="false"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>

</manifest>
```

- [ ] **Step 5: Create the `PanicApp` Application class**

`app/src/main/java/com/intellica/panicshield/PanicApp.kt`:
```kotlin
package com.intellica.panicshield

import android.app.Application

class PanicApp : Application()
```

- [ ] **Step 6: Build and confirm compilation**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Manual smoke test on a real device** (emulators can't reliably test hardware keys + accessibility services together)

1. `./gradlew installDebug`
2. On device: Settings → Accessibility → Panic Shield → enable.
3. Open the app, return to home screen.
4. Press Volume Up 3 times within 2 seconds.
5. Expected: device locks. Vibration buzzes once.

- [ ] **Step 8: Commit**

```powershell
git add .
git commit -m "feat(service): AccessibilityService wires PressTracker to lock action"
```

---

### Task 7: MainActivity + permission status check

**Files:**
- Modify: `app/src/main/java/com/intellica/panicshield/MainActivity.kt`
- Create: `app/src/main/java/com/intellica/panicshield/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/intellica/panicshield/ui/AccessibilityStatus.kt`

- [ ] **Step 1: Implement accessibility status detector**

`app/src/main/java/com/intellica/panicshield/ui/AccessibilityStatus.kt`:
```kotlin
package com.intellica.panicshield.ui

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.intellica.panicshield.service.PanicAccessibilityService

object AccessibilityStatus {

    fun isEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${PanicAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
```

- [ ] **Step 2: Implement HomeScreen**

`app/src/main/java/com/intellica/panicshield/ui/home/HomeScreen.kt`:
```kotlin
package com.intellica.panicshield.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.intellica.panicshield.ui.AccessibilityStatus

@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(AccessibilityStatus.isEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = AccessibilityStatus.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Panic Shield", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (enabled) "Active. Volume up x3 will lock." else "Inactive. Grant accessibility access.",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!enabled) {
            Button(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text("Open Accessibility Settings") }
        }
        TextButton(onClick = onOpenSettings) { Text("Settings") }
    }
}
```

- [ ] **Step 3: Wire MainActivity**

`app/src/main/java/com/intellica/panicshield/MainActivity.kt`:
```kotlin
package com.intellica.panicshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.intellica.panicshield.ui.home.HomeScreen
import com.intellica.panicshield.ui.settings.SettingsScreen
import com.intellica.panicshield.ui.theme.PanicShieldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PanicShieldTheme {
                val showSettings = remember { mutableStateOf(false) }
                if (showSettings.value) {
                    SettingsScreen(onBack = { showSettings.value = false })
                } else {
                    HomeScreen(onOpenSettings = { showSettings.value = true })
                }
            }
        }
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. (`SettingsScreen` is referenced — next task creates it. To make this task self-contained, temporarily add a stub `SettingsScreen.kt` file OR jump to Task 8 first. Recommend: do Task 8 first.)

> **Note:** Task 7 and Task 8 are interdependent — implement them as a pair without a build/test step between them. Commit only after both compile together.

- [ ] **Step 5:** Skip commit; proceed to Task 8.

---

### Task 8: SettingsScreen

**Files:**
- Create: `app/src/main/java/com/intellica/panicshield/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/intellica/panicshield/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Implement ViewModel**

`app/src/main/java/com/intellica/panicshield/ui/settings/SettingsViewModel.kt`:
```kotlin
package com.intellica.panicshield.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.intellica.panicshield.action.AccessibilityLockAction
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.settings.TriggerConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val config: StateFlow<TriggerConfig> = repo.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TriggerConfig.DEFAULT,
    )

    fun setEnabled(value: Boolean) = update { it.copy(enabled = value) }
    fun setPressCount(value: Int) = update { it.copy(pressCount = value) }
    fun setWindowMs(value: Long) = update { it.copy(windowMs = value) }
    fun setVibrate(value: Boolean) = update { it.copy(vibrate = value) }

    private fun update(transform: (TriggerConfig) -> TriggerConfig) {
        viewModelScope.launch { repo.update(transform) }
    }
}
```

- [ ] **Step 2: Implement SettingsScreen**

`app/src/main/java/com/intellica/panicshield/ui/settings/SettingsScreen.kt`:
```kotlin
package com.intellica.panicshield.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intellica.panicshield.settings.TriggerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val config by viewModel.config.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
        )
    }) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = config.enabled, onCheckedChange = viewModel::setEnabled)
            }

            Text("Press count: ${config.pressCount}")
            Slider(
                value = config.pressCount.toFloat(),
                onValueChange = { viewModel.setPressCount(it.toInt()) },
                valueRange = TriggerConfig.MIN_PRESS_COUNT.toFloat()..TriggerConfig.MAX_PRESS_COUNT.toFloat(),
                steps = TriggerConfig.MAX_PRESS_COUNT - TriggerConfig.MIN_PRESS_COUNT - 1,
            )

            Text("Window: ${"%.1f".format(config.windowMs / 1000.0)} s")
            Slider(
                value = config.windowMs.toFloat(),
                onValueChange = { viewModel.setWindowMs(it.toLong()) },
                valueRange = TriggerConfig.MIN_WINDOW_MS.toFloat()..TriggerConfig.MAX_WINDOW_MS.toFloat(),
                steps = 5,
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Vibrate on trigger", modifier = Modifier.weight(1f))
                Switch(checked = config.vibrate, onCheckedChange = viewModel::setVibrate)
            }
        }
    }
}
```

- [ ] **Step 3: Build + verify**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual sanity check on device**

1. Install: `./gradlew installDebug`
2. Open app — Home screen should show "Inactive" if accessibility not yet granted, "Active" once granted.
3. Tap "Settings" — verify sliders move, switches toggle, values persist across app restart.

- [ ] **Step 5: Commit (Tasks 7+8 together)**

```powershell
git add .
git commit -m "feat(ui): Home + Settings screens with accessibility-status detection"
```

---

### Task 9: Onboarding screen

**Files:**
- Create: `app/src/main/java/com/intellica/panicshield/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/intellica/panicshield/MainActivity.kt`
- Modify: `app/src/main/java/com/intellica/panicshield/settings/SettingsRepository.kt` (add an `onboardingDone` flag)

- [ ] **Step 1: Add `onboardingDone` to settings**

In `app/src/main/java/com/intellica/panicshield/settings/SettingsRepository.kt`, add to the class:
```kotlin
private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }

suspend fun markOnboardingDone() {
    context.dataStore.edit { it[ONBOARDING_DONE] = true }
}
```

- [ ] **Step 2: Implement Onboarding**

`app/src/main/java/com/intellica/panicshield/ui/onboarding/OnboardingScreen.kt`:
```kotlin
package com.intellica.panicshield.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        "Three rapid volume-up presses lock your screen." to
            "Nothing else. No tracking, no network, no cloud.",
        "Panic Shield needs Accessibility permission" to
            "Only to listen for the Volume Up hardware key. It does NOT read on-screen content.",
        "Test it any time." to
            "When you're ready, tap Continue. We'll open the Accessibility settings.",
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { i ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(pages[i].first, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Text(pages[i].second, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (pager.currentPage < pages.lastIndex) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    onDone()
                }
            },
        ) {
            Text(if (pager.currentPage < pages.lastIndex) "Next" else "Continue")
        }
    }
}
```

- [ ] **Step 3: Wire onboarding into MainActivity**

Replace `MainActivity.kt`:
```kotlin
package com.intellica.panicshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.ui.home.HomeScreen
import com.intellica.panicshield.ui.onboarding.OnboardingScreen
import com.intellica.panicshield.ui.settings.SettingsScreen
import com.intellica.panicshield.ui.theme.PanicShieldTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = SettingsRepository(applicationContext)
        setContent {
            PanicShieldTheme {
                var stage by remember { mutableStateOf<Stage?>(null) }
                LaunchedEffect(Unit) {
                    stage = if (repo.onboardingDone.first()) Stage.Home else Stage.Onboarding
                }
                when (stage) {
                    Stage.Onboarding -> OnboardingScreen(onDone = {
                        lifecycleScope.launch {
                            repo.markOnboardingDone()
                            stage = Stage.Home
                        }
                    })
                    Stage.Home -> {
                        var showSettings by remember { mutableStateOf(false) }
                        if (showSettings) SettingsScreen(onBack = { showSettings = false })
                        else HomeScreen(onOpenSettings = { showSettings = true })
                    }
                    null -> { /* loading — show nothing for first paint */ }
                }
            }
        }
    }

    private enum class Stage { Onboarding, Home }
}
```

- [ ] **Step 4: Build + manual verify**

Run: `./gradlew assembleDebug && ./gradlew installDebug`
Expected: First launch → onboarding. Continue → accessibility settings. Reopen app → straight to home.

- [ ] **Step 5: Commit**

```powershell
git add .
git commit -m "feat(ui): onboarding flow with accessibility deep-link"
```

---

### Task 10: Battery optimization exemption prompt

**Files:**
- Modify: `app/src/main/java/com/intellica/panicshield/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/intellica/panicshield/ui/BatteryOptimization.kt`

OEMs (Xiaomi/Huawei/Samsung) aggressively kill background services. Accessibility services survive better than foreground services, but exempting from battery optimization further hardens reliability.

- [ ] **Step 1: Implement helper**

`app/src/main/java/com/intellica/panicshield/ui/BatteryOptimization.kt`:
```kotlin
package com.intellica.panicshield.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimization {

    fun isIgnoring(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestExemption(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
```

- [ ] **Step 2: Surface the prompt on the home screen when accessibility is granted but battery is restricted**

In `HomeScreen.kt`, inside the existing `Column`, add (after the existing buttons):
```kotlin
val batteryOk = remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }
DisposableEffect(lifecycleOwner) {
    val obs = LifecycleEventObserver { _, e ->
        if (e == Lifecycle.Event.ON_RESUME) batteryOk.value = BatteryOptimization.isIgnoring(context)
    }
    lifecycleOwner.lifecycle.addObserver(obs)
    onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
}
if (enabled && !batteryOk.value) {
    Button(onClick = { BatteryOptimization.requestExemption(context) }) {
        Text("Disable battery optimization")
    }
}
```

(Add the import `import com.intellica.panicshield.ui.BatteryOptimization` at the top.)

- [ ] **Step 3: Build + manual verify**

Run: `./gradlew assembleDebug installDebug`. Confirm prompt appears, tapping it opens the system dialog, dismissing returns to app and button disappears.

- [ ] **Step 4: Commit**

```powershell
git add .
git commit -m "feat(ui): battery-optimization exemption prompt"
```

---

### Task 11: App icon + theme + branding

**Files:**
- Replace: `app/src/main/res/mipmap-*/ic_launcher*.{png,xml}`
- Modify: `app/src/main/java/com/intellica/panicshield/ui/theme/Color.kt`, `Theme.kt`

**Approach:** Use Android Studio → Image Asset Studio to generate a launcher icon. Source: a simple shield-with-up-arrow vector. SVG can be sketched in Figma or hand-coded.

- [ ] **Step 1: Sketch an icon**

In Figma (or paper → photo), draw a 192×192 shield outline with a single chevron-up inside. Export as 512×512 PNG.

- [ ] **Step 2: Generate launcher icons**

Android Studio → right-click `app/src/main/res` → New → Image Asset → Launcher Icons (Adaptive and Legacy) → upload PNG → Next → Finish.

- [ ] **Step 3: Tweak theme colors**

`app/src/main/java/com/intellica/panicshield/ui/theme/Color.kt`:
```kotlin
package com.intellica.panicshield.ui.theme

import androidx.compose.ui.graphics.Color

val ShieldRed = Color(0xFFD32F2F)
val ShieldRedDark = Color(0xFFB71C1C)
val NeutralDark = Color(0xFF1A1A1A)
```

In `Theme.kt`, swap the primary color of both dark and light schemes to `ShieldRed`.

- [ ] **Step 4: Commit**

```powershell
git add .
git commit -m "feat(ui): launcher icon + shield-red theme"
```

---

### Task 12: Privacy policy + Play Data Safety draft

**Files:**
- Create: `PRIVACY.md`
- Create: `docs/data-safety.md`

Required by Google Play even for apps with no data collection.

- [ ] **Step 1: Write PRIVACY.md**

`PRIVACY.md`:
```markdown
# Panic Shield — Privacy Policy

**Last updated:** 2026-05-27

Panic Shield is an Android application whose only function is to detect three rapid presses of the Volume Up hardware key and immediately lock the device screen.

## Data we collect

**None.** Panic Shield does not collect, store, or transmit any user data. The app contains no analytics SDK, no crash reporter, no advertising library, and has no `INTERNET` permission.

## Permissions

- **Accessibility Service (BIND_ACCESSIBILITY_SERVICE):** required to receive Volume Up key events while the app is in the background and to invoke the system "lock screen" global action. Panic Shield does not read on-screen content. The service uses `flagRequestFilterKeyEvents` exclusively and only reacts to `KEYCODE_VOLUME_UP`.
- **VIBRATE:** to buzz briefly when the trigger fires (toggleable in settings).
- **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:** to ask the user to exempt Panic Shield from battery optimization so the service remains reliable.

## Contact

kerem.turkyilmaz@intellica.net
```

- [ ] **Step 2: Write Play Data Safety values**

`docs/data-safety.md`:
```markdown
# Play Data Safety — Panic Shield

- Does your app collect or share any of the required user data types? **No**
- Is all user data encrypted in transit? **N/A** (no data leaves device)
- Do you provide a way for users to request that their data be deleted? **N/A** (no data collected)

Sensitive permissions in use:
- Accessibility Service — declared with policy: "Used solely to detect Volume Up hardware key presses and invoke the system lock-screen global action. Does not read on-screen content. Required for app's sole feature."
```

- [ ] **Step 3: Commit**

```powershell
git add PRIVACY.md docs/data-safety.md
git commit -m "docs: privacy policy + Play data-safety draft"
```

---

### Task 13: Manual E2E checklist

**Files:**
- Create: `docs/qa-checklist.md`

- [ ] **Step 1: Write the checklist**

`docs/qa-checklist.md`:
```markdown
# Panic Shield — Manual QA Checklist

Run before every Closed Test build. ~10 minutes on a real device.

## Setup
- [ ] Fresh install (uninstall any prior version)
- [ ] First launch shows onboarding (3 pages)
- [ ] "Continue" opens Accessibility settings
- [ ] Enable Panic Shield in Accessibility settings → return to app
- [ ] Home screen shows "Active"

## Core trigger
- [ ] Lock the device manually, screen off → press Volume Up x3 within 2 seconds → device lock state preserved (no crash, but Volume Up does nothing — expected, accessibility services don't run on lockscreen)
- [ ] App in foreground → Volume Up x3 within 2s → screen locks immediately, vibration buzzes
- [ ] Home screen (launcher) → Volume Up x3 within 2s → screen locks
- [ ] Other app foreground (Chrome) → Volume Up x3 within 2s → screen locks
- [ ] Music playing → Volume Up x3 → screen locks; volume continues to rise normally (we do NOT consume the event)

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

## OEM coverage (record results)
- [ ] Pixel (stock Android)
- [ ] Samsung One UI
- [ ] Xiaomi MIUI / HyperOS
- [ ] Optionally: Huawei EMUI, OPPO ColorOS
```

- [ ] **Step 2: Commit**

```powershell
git add docs/qa-checklist.md
git commit -m "docs: manual QA checklist for Closed Test builds"
```

---

### Task 14: Play Store onboarding (out-of-code; mirror ScanQuiet §6)

This task has **no code**. It runs in parallel with the build phases above.

- [ ] Use the existing Play Console account (already verified during ScanQuiet onboarding — no need to repeat)
- [ ] Create a new app in the console: `Panic Shield`, category Tools
- [ ] Internal Test track: upload first signed AAB (`./gradlew :app:bundleRelease` after configuring signing in `~/.gradle/gradle.properties`)
- [ ] Recruit 12 testers (reuse ScanQuiet's tester pool — same Telegram group; offer "early supporter" credit)
- [ ] Closed Test 14 days
- [ ] Production apply
- [ ] **Listing assets:**
  - App icon (the one from Task 11, scaled to 512×512)
  - Feature graphic (1024×500) — shield silhouette + tagline
  - 5–8 phone screenshots (Onboarding, Home active, Settings, Trigger demo with overlay text)
  - Short description (80 char): "Volume up three times. Lock now."
  - Full description (4000 char) — 1 paragraph what it does, 1 paragraph privacy stance, 1 paragraph limitations (no biometric-disable)
  - Release notes
- [ ] Data Safety form (use `docs/data-safety.md`)
- [ ] Privacy Policy URL — host `PRIVACY.md` as GitHub Pages or a Cloudflare Pages static site

---

## 3. Risks & open items

| # | Risk | Mitigation |
|---|---|---|
| R1 | OEMs may strip accessibility permission after reboot (Xiaomi/Huawei) | Document in listing + onboarding warning. Add v1.1 "verify still active" notification. |
| R2 | Volume key events don't reach accessibility service on locked screen → no panic trigger from pocket-lock | Known limit. Document honestly. v1.1: add lockscreen-bypass via MediaSession media-button receiver. |
| R3 | Play review flags Accessibility Service as out-of-policy | Listing must justify the permission per Google's Accessibility policy. The text in Task 12 §2 is the justification — file it as part of the in-app permission rationale and Play store form. |
| R4 | OEM `keyCode` mapping differences | All 5 OEMs in the test matrix (Task 13) use standard `KEYCODE_VOLUME_UP`. Confirmed by tester reports during Closed Test. |
| R5 | Premature accessibility permission revocation by user | Home screen status check (Task 7) makes this obvious. |

## 4. Definition of Done (mirrors §0.7)

- [ ] All Tasks 1–14 complete
- [ ] Manual QA checklist passes on at least 3 OEMs
- [ ] Production live on Play
- [ ] Crash-free ≥99% over first 14 days
- [ ] At least 1 organic install with a rating

---
