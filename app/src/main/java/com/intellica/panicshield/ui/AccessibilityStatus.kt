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
