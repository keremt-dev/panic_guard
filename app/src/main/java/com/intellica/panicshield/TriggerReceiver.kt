package com.intellica.panicshield

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.intellica.panicshield.service.PanicAccessibilityService

/**
 * Broadcast entry point for automation tools (Tasker, MacroDroid, `am
 * broadcast`). Unlike [TriggerActivity], a broadcast is not subject to the
 * background-activity-start restriction, so this fires reliably even while the
 * screen is locked — useful for shake/button/NFC automations that arm the SOS
 * SMS and safeguards without the phone being unlocked.
 *
 * Send with:
 *   adb shell am broadcast -a com.intellica.panicshield.action.TRIGGER \
 *     -n com.intellica.panicshield/.TriggerReceiver
 */
class TriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = PanicAccessibilityService.instance
        if (service != null) {
            Log.d("PanicTrigger", "broadcast trigger -> fireExternal")
            service.fireExternal()
        } else {
            Log.w("PanicTrigger", "broadcast trigger but accessibility service not running")
        }
    }

    companion object {
        const val ACTION_TRIGGER = "com.intellica.panicshield.action.TRIGGER"
    }
}
