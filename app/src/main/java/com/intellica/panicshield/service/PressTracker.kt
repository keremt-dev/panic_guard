package com.intellica.panicshield.service

class PressTracker(
    private val requiredPresses: Int,
    private val windowMs: Long,
) {
    private val timestamps = ArrayDeque<Long>()

    fun record(timeMs: Long): Boolean {
        timestamps.addLast(timeMs)
        while (timestamps.isNotEmpty() && timeMs - timestamps.first() > windowMs) {
            timestamps.removeFirst()
        }
        return if (timestamps.size >= requiredPresses) {
            timestamps.clear()
            true
        } else {
            false
        }
    }
}
