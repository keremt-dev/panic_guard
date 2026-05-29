package com.intellica.panicshield.panic

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import com.intellica.panicshield.action.AccessibilityLockAction
import com.intellica.panicshield.settings.TriggerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Single entry point for "panic just happened — do everything."
 *
 * Each future v1.1 reactor (SOS SMS, FaceCapture, LockdownHack, BankBlocker
 * re-arm) plugs into [fire] sequentially. Reactors that do real work
 * (network, camera, location) are dispatched to their own foreground
 * services from here; this method itself stays cheap and returns fast so
 * the AccessibilityService's onKeyEvent doesn't block the input dispatcher.
 */
class PanicCoordinator(
    private val service: AccessibilityService,
    private val stateRepo: PanicStateRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun fire(config: TriggerConfig) {
        Log.d(TAG, "fire()")

        // 1. Mark panic ACTIVE (durable flag for cross-process consumers)
        scope.launch {
            stateRepo.activate(now = SystemClock.elapsedRealtime())
        }

        // 2. Lock the screen — preserved v1.0 behavior
        AccessibilityLockAction(service = service, vibrate = config.vibrate).lockNow()

        // 3. Future reactors (added in later tasks):
        //    - LockdownHack.fire()       (Task 19)
        //    - SosSmsReactor start       (Task 16)
        //    - FaceCaptureService start  (Task 17)
        //    - BankBlocker is event-driven, just observes state (Task 18)
    }

    private companion object {
        const val TAG = "PanicCoord"
    }
}
