package com.intellica.panicshield.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PressTrackerTest {

    @Test
    fun `three presses within window fires`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        assertThat(tracker.record(timeMs = 0L)).isFalse()
        assertThat(tracker.record(timeMs = 500L)).isFalse()
        assertThat(tracker.record(timeMs = 1000L)).isTrue()
    }

    @Test
    fun `three presses outside window does not fire`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        tracker.record(timeMs = 0L)
        tracker.record(timeMs = 1500L)
        assertThat(tracker.record(timeMs = 3000L)).isFalse()
    }

    @Test
    fun `presses outside window expire and do not count`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        tracker.record(timeMs = 0L)
        tracker.record(timeMs = 2200L)
        tracker.record(timeMs = 2400L)
        assertThat(tracker.record(timeMs = 2600L)).isTrue()
    }

    @Test
    fun `after firing tracker resets so next trigger needs full sequence`() {
        val tracker = PressTracker(requiredPresses = 3, windowMs = 2000)
        tracker.record(timeMs = 0L)
        tracker.record(timeMs = 500L)
        tracker.record(timeMs = 1000L)
        assertThat(tracker.record(timeMs = 1100L)).isFalse()
        assertThat(tracker.record(timeMs = 1200L)).isFalse()
        assertThat(tracker.record(timeMs = 1300L)).isTrue()
    }

    @Test
    fun `count of 2 within window fires`() {
        val tracker = PressTracker(requiredPresses = 2, windowMs = 1000)
        assertThat(tracker.record(timeMs = 0L)).isFalse()
        assertThat(tracker.record(timeMs = 500L)).isTrue()
    }
}
