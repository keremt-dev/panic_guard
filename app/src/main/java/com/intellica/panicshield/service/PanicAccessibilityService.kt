package com.intellica.panicshield.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.intellica.panicshield.block.BankBlocker
import com.intellica.panicshield.block.ProtectedAppsRepository
import com.intellica.panicshield.camera.FaceCaptureService
import com.intellica.panicshield.panic.PanicCoordinator
import com.intellica.panicshield.panic.PanicState
import com.intellica.panicshield.panic.PanicStateRepository
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.settings.TriggerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val TAG = "PanicAS"
private const val BLOCK_COOLDOWN_MS = 1200L

class PanicAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settings: SettingsRepository
    private lateinit var coordinator: PanicCoordinator
    private lateinit var protectedApps: ProtectedAppsRepository
    private var tracker: PressTracker = PressTracker(
        TriggerConfig.DEFAULT.pressCount,
        TriggerConfig.DEFAULT.windowMs,
    )
    private var currentConfig: TriggerConfig = TriggerConfig.DEFAULT

    // Mirrored from flows so onAccessibilityEvent (hot path) stays allocation-free.
    @Volatile private var panicActive: Boolean = false
    @Volatile private var protectedPackages: Set<String> = emptySet()
    @Volatile private var captureOnTrigger: Boolean = true

    // Block debounce: a launching protected app emits a burst of window events;
    // firing GLOBAL_ACTION_HOME on every one hammers the system and trips
    // aggressive OEM watchdogs (HyperOS) into killing the service. One HOME per
    // package per cooldown is enough to keep the app out.
    private var lastBlockedPkg: String? = null
    private var lastBlockAtMs: Long = 0L

    /**
     * Fires whenever the screen wakes or the keyguard is dismissed. While panic
     * is active this is exactly when the attacker's face is in front of the
     * phone, so we trigger the silent capture here (not at panic-fire time,
     * when the victim is the one looking at the screen).
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!panicActive || !captureOnTrigger) return
            Log.d(TAG, "screen event ${intent.action} while panic active -> capture")
            startCapture()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        settings = SettingsRepository(applicationContext)
        protectedApps = ProtectedAppsRepository(applicationContext)
        val stateRepo = PanicStateRepository(applicationContext)
        coordinator = PanicCoordinator(
            service = this,
            settingsRepo = settings,
            stateRepo = stateRepo,
        )

        settings.config
            .onEach { config ->
                Log.d(TAG, "config updated: $config")
                currentConfig = config
                tracker = PressTracker(config.pressCount, config.windowMs)
            }
            .launchIn(scope)

        stateRepo.state
            .onEach { panicActive = it == PanicState.ACTIVE }
            .launchIn(scope)

        protectedApps.protectedPackages
            .onEach { protectedPackages = it }
            .launchIn(scope)

        settings.captureOnTrigger
            .onEach { captureOnTrigger = it }
            .launchIn(scope)

        registerScreenReceiver()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun startCapture() {
        val intent = FaceCaptureService.startIntent(applicationContext)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "failed to start capture", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "onServiceConnected; serviceInfo=${serviceInfo?.flags}")
    }

    /** Fired by an external trigger (voice / App Action / shortcut / deep link)
     *  via [TriggerActivity]. Runs the full panic sequence regardless of the
     *  volume-key 'enabled' flag, since the user invoked it explicitly. */
    fun fireExternal() {
        Log.d(TAG, "fireExternal()")
        coordinator.fire(currentConfig)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!currentConfig.enabled) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.repeatCount != 0) return false

        val fired = tracker.record(event.eventTime)
        Log.d(TAG, "VOLUME_UP recorded -> fired=$fired")
        if (fired) {
            Log.d(TAG, "FIRING coordinator")
            coordinator.fire(currentConfig)
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()
        val blocker = BankBlocker(protectedPackages = protectedPackages, panicActive = panicActive)
        if (!blocker.shouldBlock(pkg)) return

        val now = android.os.SystemClock.elapsedRealtime()
        if (pkg == lastBlockedPkg && now - lastBlockAtMs < BLOCK_COOLDOWN_MS) return
        lastBlockedPkg = pkg
        lastBlockAtMs = now
        Log.d(TAG, "blocking protected app: $pkg")
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (instance === this) instance = null
        runCatching { unregisterReceiver(screenReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Set while the service is connected; used by TriggerActivity to fire
         *  the panic sequence from an external (voice/shortcut) entry point. */
        @Volatile
        var instance: PanicAccessibilityService? = null
            private set
    }
}
