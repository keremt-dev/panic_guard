package com.intellica.panicshield.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens this app's system "App info" page, where OEM reliability controls
 *  (Autostart, Battery saver / no-restrictions) live. There is no universal
 *  intent for OEM autostart, so we route the user here with instructions. */
object AppSettings {
    fun openAppInfo(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
