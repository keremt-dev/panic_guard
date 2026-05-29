package com.intellica.panicshield.service

import java.util.ArrayDeque

/**
 * Pure shake-detection logic (no Android sensor deps, so it's unit-testable).
 *
 * Feed it the accelerometer magnitude expressed in g (m/s² ÷ 9.81). A "spike"
 * is a sample above [gThreshold], debounced by [minGapMs] so one violent jerk
 * counts once. When [requiredShakes] spikes land within [windowMs], it fires
 * (and resets) — requiring several spikes avoids a single bump triggering panic.
 */
class ShakeDetector(
    private val gThreshold: Float,
    private val requiredShakes: Int,
    private val windowMs: Long,
    private val minGapMs: Long = 120L,
) {
    private val spikes = ArrayDeque<Long>()
    private var lastSpikeMs: Long? = null

    fun onSample(gForce: Float, timeMs: Long): Boolean {
        if (gForce < gThreshold) return false
        val last = lastSpikeMs
        if (last != null && timeMs - last < minGapMs) return false
        lastSpikeMs = timeMs

        spikes.addLast(timeMs)
        while (spikes.isNotEmpty() && timeMs - spikes.first() > windowMs) {
            spikes.removeFirst()
        }
        return if (spikes.size >= requiredShakes) {
            spikes.clear()
            true
        } else {
            false
        }
    }
}
