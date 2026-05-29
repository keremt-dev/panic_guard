package com.intellica.panicshield.block

/**
 * Pure decision logic: should the foreground app be kicked to home?
 *
 * Blocks only when panic is ACTIVE and the foreground package is in the
 * user's protected set. Kept free of Android dependencies so it's unit
 * testable.
 */
class BankBlocker(
    private val protectedPackages: Set<String>,
    private val panicActive: Boolean,
) {
    fun shouldBlock(packageName: String?): Boolean =
        panicActive && packageName != null && packageName in protectedPackages
}
