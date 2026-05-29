package com.intellica.panicshield.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "FaceCapture"
private const val NOTIF_ID = 4712
private const val NOTIF_CHANNEL_ID = "panic_shield_capture"
private const val WINDOW_MS = 8_000L
// Capture is only triggered on screen-wake / unlock, so a person is present
// by definition. We grab a frame from the ImageAnalysis stream (which works
// on devices where ImageCapture still-capture doesn't, e.g. emulator webcam):
// if ML Kit confirms a face sooner we grab that frame; otherwise we grab one
// after this delay regardless so we never miss the attacker.
private const val FALLBACK_CAPTURE_MS = 1_800L
private const val JPEG_QUALITY = 90

/**
 * Foreground service (type=camera) that grabs a single silent front-camera
 * photo into app-private storage, then stops. No PreviewView is bound — the
 * capture is invisible. Started by PanicAccessibilityService when the phone
 * is woken / unlocked while panic is active.
 *
 * The photo comes from the ImageAnalysis frame stream (not ImageCapture),
 * because the still-capture pipeline is unreliable on some camera providers
 * (notably the emulator's host-webcam bridge) while the analysis stream works.
 */
class FaceCaptureService : LifecycleService() {

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val captured = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val captureRequested = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null

    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!started.compareAndSet(false, true)) {
            Log.d(TAG, "capture already running; ignoring duplicate start")
            return START_NOT_STICKY
        }
        ensureChannel()
        // Android 14+ throws SecurityException from startForeground(camera) when
        // started from background without an exemption; that would crash the
        // process (and the AccessibilityService with it). Guard + bail cleanly.
        if (!promoteToForeground()) {
            Log.w(TAG, "could not enter camera foreground; aborting capture")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        startCamera()
        // After a short settle delay, request a capture even if no face was
        // confirmed (a person is present on unlock).
        mainHandler.postDelayed({
            if (!captured.get()) {
                Log.d(TAG, "requesting fallback capture")
                captureRequested.set(true)
            }
        }, FALLBACK_CAPTURE_MS)
        mainHandler.postDelayed({ if (!captured.get()) finish("timeout") }, WINDOW_MS)
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(NOTIF_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Panic Shield capture",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shown briefly while Panic Shield captures a photo." }
        )
    }

    /** @return true if foreground entry succeeded; false if the OS rejected it. */
    private fun promoteToForeground(): Boolean {
        val notif: Notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Panic Shield")
            .setContentText("Securing evidence…")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            } else {
                startForeground(NOTIF_ID, notif)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground(camera) rejected", e)
            false
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, ::analyze) }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
                Log.d(TAG, "camera bound (front)")
            } catch (e: Exception) {
                Log.e(TAG, "camera bind failed", e)
                finish("bind-failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        try {
            if (captured.get()) return

            // Time to capture (fallback fired or a face was confirmed): save
            // this frame synchronously from the analysis stream.
            if (captureRequested.get()) {
                saveAndFinish(proxy)
                return
            }

            // Otherwise run best-effort face detection; a positive hit makes us
            // capture the NEXT frame sooner than the fallback timer.
            val mediaImage = proxy.image ?: return
            val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
            // process() is async and keeps the underlying image, so we must NOT
            // close the proxy until it completes — done in the lambda below.
            val pending = proxy
            faceDetector.process(input)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        Log.d(TAG, "face detected (${faces.size}); will capture next frame")
                        captureRequested.set(true)
                    }
                }
                .addOnFailureListener { Log.e(TAG, "face process failed", it) }
                .addOnCompleteListener { pending.close() }
            return  // proxy closed in the completion lambda
        } catch (e: Exception) {
            Log.e(TAG, "analyze error", e)
        }
        proxy.close()
    }

    private fun saveAndFinish(proxy: ImageProxy) {
        if (!captured.compareAndSet(false, true)) return
        try {
            val rotated = rotate(proxy.toBitmap(), proxy.imageInfo.rotationDegrees)
            val file = CaptureStorage.newFile(filesDir, System.currentTimeMillis())
            FileOutputStream(file).use { rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            Log.d(TAG, "saved: ${file.absolutePath} (${rotated.width}x${rotated.height})")
            finish("captured")
        } catch (e: Exception) {
            Log.e(TAG, "save failed", e)
            finish("save-error")
        }
    }

    private fun rotate(bmp: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bmp
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun finish(reason: String) {
        mainHandler.post {
            Log.d(TAG, "finish: $reason")
            runCatching { cameraProvider?.unbindAll() }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        analysisExecutor.shutdown()
        faceDetector.close()
        super.onDestroy()
    }

    companion object {
        fun startIntent(context: Context): Intent =
            Intent(context, FaceCaptureService::class.java)
    }
}
