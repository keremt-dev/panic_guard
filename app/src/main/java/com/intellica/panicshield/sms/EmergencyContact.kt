package com.intellica.panicshield.sms

/**
 * The single person Panic Shield will SMS on trigger.
 *
 * [phoneE164] should be normalized to E.164 (e.g. "+905321234567") via
 * [android.telephony.PhoneNumberUtils.formatNumberToE164] when picked.
 */
data class EmergencyContact(
    val displayName: String,
    val phoneE164: String,
)
