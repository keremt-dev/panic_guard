package com.intellica.panicshield.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "FaceCapture"
private const val NOTIF_ID = 4712
private const val NOTIF_CHANNEL_ID = "panic_shield_capture"
private const val WINDOW_MS = 12_000L
// Since capture is now only triggered on screen-wake / unlock (a person is
// present by definition), face detection is a best-effort nicety: if a face
// is found before this deadline we snap immediately; otherwise we snap anyway
// so we never miss the attacker just because ML Kit was slow to confirm a face.
private const val FALLBACK_CAPTURE_MS = 2_500L

/**
 * Foreground service (type=camera) that, for up to 5 seconds, watches the
 * FRONT camera for a human face. On the first frame with a face above the
 * confidence threshold it snaps a full-resolution still into app-private
 * storage, then stops. No PreviewView is bound — capture is invisible.
 *
 * If no face appears within the window, nothing is saved (a photo of the
 * ceiling/pocket is useless).
 */
class FaceCaptureService : LifecycleService() {

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val captured = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // A rapid double-trigger (user pressing volume-up more than the
        // required count) would start this twice and the second unbindAll()
        // would tear down the first session's frame stream. Ignore re-entry.
        if (!started.compareAndSet(false, true)) {
            Log.d(TAG, "capture already running; ignoring duplicate start")
            return START_NOT_STICKY
        }
        ensureChannel()
        // Android 14+ blocks starting a camera-type FGS from the background
        // (anti-spyware). startForeground() then throws SecurityException
        // INSIDE this callback, which would crash the whole process — taking
        // down the AccessibilityService with it. Guard it and bail cleanly.
        if (!promoteToForeground()) {
            Log.w(TAG, "could not enter camera foreground; aborting capture")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        startCamera()
        // Best-effort fallback: if no face has been captured yet, snap anyway.
        mainHandler.postDelayed({
            if (!captured.get()) {
                Log.d(TAG, "fallback capture (no face confirmed in time)")
                takePhoto()
            }
        }, FALLBACK_CAPTURE_MS)
        mainHandler.postDelayed({ finish("timeout") }, WINDOW_MS)
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

    /** @return true if we successfully entered the foreground; false if the
     *  OS rejected the camera FGS (e.g. background start on API 34+). */
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

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    analysis,
                    capture,
                )
                Log.d(TAG, "camera bound (front)")
            } catch (e: Exception) {
                Log.e(TAG, "camera bind failed", e)
                finish("bind-failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val mediaImage = proxy.image
        Log.d(TAG, "analyze() called; image=${if (mediaImage == null) "null" else "ok"} captured=${captured.get()}")
        if (mediaImage == null || captured.get()) {
            proxy.close()
            return
        }
        val rotation = proxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)
        faceDetector.process(input)
            .addOnSuccessListener { faces ->
                Log.d(TAG, "frame ${input.width}x${input.height} rot=$rotation faces=${faces.size}")
                if (faces.isNotEmpty() && !captured.get()) {
                    Log.d(TAG, "face detected (${faces.size}); capturing")
                    takePhoto()
                }
            }
            .addOnFailureListener { Log.e(TAG, "face process failed", it) }
            .addOnCompleteListener { proxy.close() }
    }

    private fun takePhoto() {
        if (!captured.compareAndSet(false, true)) return
        val capture = imageCapture ?: return finish("no-capture-usecase")
        val now = System.currentTimeMillis()
        val file = CaptureStorage.newFile(filesDir, now)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            analysisExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "saved: ${file.absolutePath}")
                    finish("captured")
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "capture error", exc)
                    finish("capture-error")
                }
            },
        )
    }

    private fun finish(reason: String) {
        mainHandler.post {
            Log.d(TAG, "finish: $reason")
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
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
