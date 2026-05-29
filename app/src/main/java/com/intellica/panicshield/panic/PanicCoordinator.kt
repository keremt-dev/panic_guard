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

        // 1. Mark panic ACTIVE
        scope.launch { stateRepo.activate(now = SystemClock.elapsedRealtime()) }

        // 2. Lock the screen
        AccessibilityLockAction(service = service, vibrate = config.vibrate).lockNow()

        // 3. SOS SMS (only if a contact has been chosen)
        scope.launch {
            val contact = settingsRepo.emergencyContact.first()
            if (contact == null) {
                Log.d(TAG, "no emergency contact set; skipping SMS")
                return@launch
            }
            val intent = SosSmsReactor.startIntent(appContext, contact)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }

        // 4. Future reactors:
        //    - LockdownHack.fire()       (Task 19)
        //    - FaceCaptureService start  (Task 17)
        //    - BankBlocker is event-driven, just observes state (Task 18)
    }

    private companion object {
        const val TAG = "PanicCoord"
    }
}
