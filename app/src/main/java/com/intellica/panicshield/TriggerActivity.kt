package com.intellica.panicshield

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.intellica.panicshield.service.PanicAccessibilityService

/**
 * Invisible entry point for external triggers: a launcher shortcut, a Google
 * Assistant App Action / Routine, or the `panicshield://trigger` deep link.
 *
 * It fires the panic sequence through the running AccessibilityService (the
 * only component that can lock the screen) and finishes immediately, showing
 * no UI. If accessibility access hasn't been granted the service can't run, so
 * we tell the user instead of failing silently.
 */
class TriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = PanicAccessibilityService.instance
        if (service != null) {
            service.fireExternal()
        } else {
            Toast.makeText(
                this,
                "Enable Panic Shield in Accessibility first.",
                Toast.LENGTH_LONG,
            ).show()
        }
        finish()
    }
}
