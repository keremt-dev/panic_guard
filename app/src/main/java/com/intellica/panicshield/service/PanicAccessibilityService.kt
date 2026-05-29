package com.intellica.panicshield.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.intellica.panicshield.panic.PanicCoordinator
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

class PanicAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settings: SettingsRepository
    private lateinit var coordinator: PanicCoordinator
    private var tracker: PressTracker = PressTracker(
        TriggerConfig.DEFAULT.pressCount,
        TriggerConfig.DEFAULT.windowMs,
    )
    private var currentConfig: TriggerConfig = TriggerConfig.DEFAULT

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        settings = SettingsRepository(applicationContext)
        coordinator = PanicCoordinator(
            service = this,
            settingsRepo = settings,
            stateRepo = PanicStateRepository(applicationContext),
        )
        settings.config
            .onEach { config ->
                Log.d(TAG, "config updated: $config")
                currentConfig = config
                tracker = PressTracker(config.pressCount, config.windowMs)
            }
            .launchIn(scope)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected; serviceInfo=${serviceInfo?.flags}")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d(
            TAG,
            "onKeyEvent action=${event.action} keyCode=${event.keyCode} repeat=${event.repeatCount} time=${event.eventTime}"
        )
        if (!currentConfig.enabled) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.repeatCount != 0) return false

        val fired = tracker.record(event.eventTime)
        Log.d(TAG, "tracker.record -> $fired")
        if (fired) {
            Log.d(TAG, "FIRING coordinator")
            coordinator.fire(currentConfig)
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        scope.cancel()
        super.onDestroy()
    }
}
