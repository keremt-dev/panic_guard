package com.intellica.panicshield.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TriggerConfigTest {

    @Test
    fun `default config matches MVP spec`() {
        val config = TriggerConfig.DEFAULT
        assertThat(config.enabled).isTrue()
        assertThat(config.pressCount).isEqualTo(3)
        assertThat(config.windowMs).isEqualTo(2000L)
        assertThat(config.vibrate).isTrue()
    }

    @Test
    fun `pressCount is clamped to allowed range`() {
        assertThat(TriggerConfig.DEFAULT.copy(pressCount = 1).normalized().pressCount).isEqualTo(2)
        assertThat(TriggerConfig.DEFAULT.copy(pressCount = 99).normalized().pressCount).isEqualTo(5)
    }

    @Test
    fun `windowMs is clamped to allowed range`() {
        assertThat(TriggerConfig.DEFAULT.copy(windowMs = 100L).normalized().windowMs).isEqualTo(1000L)
        assertThat(TriggerConfig.DEFAULT.copy(windowMs = 99_999L).normalized().windowMs).isEqualTo(4000L)
    }
}
