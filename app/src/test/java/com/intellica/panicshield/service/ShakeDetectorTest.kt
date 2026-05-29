package com.intellica.panicshield.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShakeDetectorTest {

    private fun detector() = ShakeDetector(gThreshold = 2.5f, requiredShakes = 3, windowMs = 1500)

    @Test
    fun `three spikes within window fires`() {
        val d = detector()
        assertThat(d.onSample(3.0f, 0L)).isFalse()
        assertThat(d.onSample(3.0f, 300L)).isFalse()
        assertThat(d.onSample(3.0f, 600L)).isTrue()
    }

    @Test
    fun `samples below threshold are ignored`() {
        val d = detector()
        assertThat(d.onSample(1.0f, 0L)).isFalse()
        assertThat(d.onSample(2.0f, 300L)).isFalse()
        assertThat(d.onSample(2.4f, 600L)).isFalse()
    }

    @Test
    fun `spikes too close together count once (debounce)`() {
        val d = detector()
        d.onSample(3.0f, 0L)
        // within minGap (120ms) -> ignored, doesn't count
        assertThat(d.onSample(3.0f, 50L)).isFalse()
        assertThat(d.onSample(3.0f, 100L)).isFalse()
        // only the first spike counted so far; need two more spaced spikes
        assertThat(d.onSample(3.0f, 300L)).isFalse()
        assertThat(d.onSample(3.0f, 500L)).isTrue()
    }

    @Test
    fun `old spikes expire outside the window`() {
        val d = detector()
        d.onSample(3.0f, 0L)
        d.onSample(3.0f, 300L)
        // third spike arrives after the first has expired (>1500ms from t=0)
        assertThat(d.onSample(3.0f, 1700L)).isFalse()
    }

    @Test
    fun `after firing it resets`() {
        val d = detector()
        d.onSample(3.0f, 0L)
        d.onSample(3.0f, 300L)
        assertThat(d.onSample(3.0f, 600L)).isTrue()   // fires, resets, lastSpike=600
        assertThat(d.onSample(3.0f, 700L)).isFalse()  // debounced (100ms < 120ms)
        assertThat(d.onSample(3.0f, 900L)).isFalse()  // fresh spike 1
        assertThat(d.onSample(3.0f, 1100L)).isFalse() // fresh spike 2
        assertThat(d.onSample(3.0f, 1300L)).isTrue()  // fresh spike 3 -> fires
    }
}
