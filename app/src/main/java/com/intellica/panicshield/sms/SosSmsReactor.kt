package com.intellica.panicshield.sms

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "SosSms"
private const val NOTIF_ID = 4711
private const val NOTIF_CHANNEL_ID = "panic_shield_sos"
private const val LOCATION_TIMEOUT_MS = 5_000L

/**
 * Foreground service (type=location) that runs once per panic event:
 * fetches a fresh GPS fix and sends an SMS to the user's chosen contact.
 *
 * Kept as a Service (not WorkManager) because (a) panic must run NOW,
 * (b) WorkManager has no "location" foreground type, (c) the work is
 * short-lived (<10s) so doze/restrictions are minor.
 */
class SosSmsReactor : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val displayName = intent?.getStringExtra(EXTRA_NAME)
        val phoneE164 = intent?.getStringExtra(EXTRA_PHONE)
        if (displayName == null || phoneE164 == null) {
            Log.w(TAG, "missing contact extras; stopping")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        ensureChannel()
        // Location FGS can be rejected when started from background on newer
        // Android. If so we DON'T crash — sending an SMS needs no FGS; we just
        // lose the fresh-GPS guarantee and fall back to last-known location.
        val inForeground = promoteToForeground()

        scope.launch {
            try {
                val location = readLocationOrNull()
                val body = composeBody(location)
                sendSms(phoneE164, body)
                Log.d(TAG, "SMS dispatched to $displayName")
            } catch (e: Exception) {
                Log.e(TAG, "send failed", e)
            } finally {
                if (inForeground) stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(NOTIF_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Panic Shield SOS",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown briefly while Panic Shield sends an SOS SMS."
            }
        )
    }

    /** @return true if foreground entry succeeded; false if the OS rejected
     *  the location FGS (we then proceed without it — SMS needs no FGS). */
    private fun promoteToForeground(): Boolean {
        val notif: Notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Sending SOS")
            .setContentText("Acquiring location and dispatching SMS…")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIF_ID, notif)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground(location) rejected", e)
            false
        }
    }

    private suspend fun readLocationOrNull(): Location? {
        if (!hasLocationPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(this)
        val cts = CancellationTokenSource()
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                suspendCancellableCoroutine<Location?> { cont ->
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            } ?: lastLocationFallback(client)
        } catch (e: SecurityException) {
            Log.w(TAG, "location permission gone mid-flight", e)
            null
        }
    }

    private suspend fun lastLocationFallback(
        client: com.google.android.gms.location.FusedLocationProviderClient,
    ): Location? = try {
        if (!hasLocationPermission()) null
        else suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    } catch (e: SecurityException) {
        null
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun sendSms(phoneE164: String, body: String) {
        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val parts = manager.divideMessage(body)
        manager.sendMultipartTextMessage(phoneE164, null, parts, null, null)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_PHONE = "extra_phone"

        fun composeBody(location: Location?): String {
            val coordsPart = if (location != null) {
                val lat = "%.6f".format(location.latitude)
                val lng = "%.6f".format(location.longitude)
                "Konum: https://maps.google.com/?q=$lat,$lng"
            } else {
                "Konum alınamadı."
            }
            return "Acil durumdayım. $coordsPart (Panic Shield)"
        }

        fun startIntent(context: Context, contact: EmergencyContact): Intent =
            Intent(context, SosSmsReactor::class.java).apply {
                putExtra(EXTRA_NAME, contact.displayName)
                putExtra(EXTRA_PHONE, contact.phoneE164)
            }
    }
}
