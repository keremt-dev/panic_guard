package com.intellica.panicshield.panic

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.intellica.panicshield.action.AccessibilityLockAction
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.settings.TriggerConfig
import com.intellica.panicshield.sms.SosSmsReactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single entry point for "panic just happened — do everything."
 *
 * Each v1.1 reactor (SOS SMS done; FaceCapture, LockdownHack, BankBlocker
 * re-arm coming) plugs into [fire] sequentially. Reactors that do real
 * work (network, camera, location) are dispatched to their own
 * foreground services from here; this method itself stays cheap and
 * returns fast so the AccessibilityService's onKeyEvent doesn't block
 * the input dispatcher.
 */
class PanicCoordinator(
    private val service: AccessibilityService,
    private val settingsRepo: SettingsRepository,
    private val stateRepo: PanicStateRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val appContext: Context = service.applicationContext

    fun fire(config: TriggerConfig) {
        Log.d(TAG, "fire()")

        // 1. Mark panic ACTIVE (durable flag observed by BankBlocker + UI)
        scope.launch { stateRepo.activate(now = SystemClock.elapsedRealtime()) }

        // 2. Camera capture is NOT started here. At panic-fire time the person
        //    in front of the camera is the victim, and the screen is about to
        //    lock (emulator/host webcam also stops feeding frames when off).
        //    Instead, PanicAccessibilityService arms a screen-on/unlock
        //    listener that captures whoever wakes or unlocks the phone while
        //    panic stays active — that's when the attacker's face is present.

        // 3. SOS SMS (only if a contact has been chosen)
        scope.launch {
            val contact = settingsRepo.emergencyContact.first()
            if (contact == null) {
                Log.d(TAG, "no emergency contact set; skipping SMS")
                return@launch
            }
            startService(SosSmsReactor.startIntent(appContext, contact))
        }

        // 4. Lock the screen last, after reactors are dispatched.
        AccessibilityLockAction(service = service, vibrate = config.vibrate).lockNow()

        // 5. Future reactors:
        //    - LockdownHack.fire()  (Task 19)
        //    - BankBlocker is event-driven, just observes state (Task 18)
    }

    private fun startService(intent: android.content.Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        } catch (e: Exception) {
            // Android 12+ can throw ForegroundServiceStartNotAllowedException
            // for camera/location FGS started from background on some OEMs.
            // Accessibility services are generally exempt, but log defensively.
            Log.e(TAG, "failed to start ${intent.component?.shortClassName}", e)
        }
    }

    private companion object {
        const val TAG = "PanicCoord"
    }
}
