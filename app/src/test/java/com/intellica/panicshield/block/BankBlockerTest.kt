package com.intellica.panicshield.block

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BankBlockerTest {

    private val banks = setOf("com.bank.a", "com.bank.b")

    @Test
    fun `does not block when panic inactive`() {
        val blocker = BankBlocker(protectedPackages = banks, panicActive = false)
        assertThat(blocker.shouldBlock("com.bank.a")).isFalse()
    }

    @Test
    fun `blocks protected package when panic active`() {
        val blocker = BankBlocker(protectedPackages = banks, panicActive = true)
        assertThat(blocker.shouldBlock("com.bank.a")).isTrue()
    }

    @Test
    fun `does not block unprotected package when panic active`() {
        val blocker = BankBlocker(protectedPackages = banks, panicActive = true)
        assertThat(blocker.shouldBlock("com.spotify.music")).isFalse()
    }

    @Test
    fun `does not block null package`() {
        val blocker = BankBlocker(protectedPackages = banks, panicActive = true)
        assertThat(blocker.shouldBlock(null)).isFalse()
    }

    @Test
    fun `does not block when protected set empty`() {
        val blocker = BankBlocker(protectedPackages = emptySet(), panicActive = true)
        assertThat(blocker.shouldBlock("com.bank.a")).isFalse()
    }
}
