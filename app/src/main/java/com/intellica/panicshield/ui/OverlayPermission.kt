package com.intellica.panicshield.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * "Display over other apps" (SYSTEM_ALERT_WINDOW). Besides drawing overlays,
 * holding this permission grants an exemption from the Android 12+ background
 * foreground-service-start restriction — which is what lets the silent camera
 * capture FGS start when panic is triggered from the background / locked screen.
 */
object OverlayPermission {

    fun isGranted(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun request(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
