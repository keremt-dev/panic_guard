package com.intellica.panicshield.settings

data class TriggerConfig(
    val enabled: Boolean,
    val pressCount: Int,
    val windowMs: Long,
    val vibrate: Boolean,
) {
    fun normalized(): TriggerConfig = copy(
        pressCount = pressCount.coerceIn(MIN_PRESS_COUNT, MAX_PRESS_COUNT),
        windowMs = windowMs.coerceIn(MIN_WINDOW_MS, MAX_WINDOW_MS),
    )

    companion object {
        const val MIN_PRESS_COUNT = 2
        const val MAX_PRESS_COUNT = 5
        const val MIN_WINDOW_MS = 1000L
        const val MAX_WINDOW_MS = 4000L

        val DEFAULT = TriggerConfig(
            enabled = true,
            pressCount = 3,
            windowMs = 2000L,
            vibrate = true,
        )
    }
}
