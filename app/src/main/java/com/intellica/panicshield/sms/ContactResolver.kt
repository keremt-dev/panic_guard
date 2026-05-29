package com.intellica.panicshield.sms

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import java.util.Locale

/**
 * Resolves a content URI returned by [android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI]
 * picker into a [EmergencyContact] with E.164-normalized phone.
 *
 * Returns null if the URI doesn't yield both a name and a phone number.
 */
object ContactResolver {

    fun resolve(resolver: ContentResolver, contactUri: Uri): EmergencyContact? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        resolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "Emergency contact"
            val rawNumber = cursor.getString(1)?.trim().orEmpty()
            if (rawNumber.isEmpty()) return null

            val region = Locale.getDefault().country.ifBlank { "TR" }
            val e164 = PhoneNumberUtils.formatNumberToE164(rawNumber, region)
                ?: fallbackNormalize(rawNumber)
                ?: return null

            return EmergencyContact(displayName = name, phoneE164 = e164)
        }
        return null
    }

    /**
     * If [PhoneNumberUtils.formatNumberToE164] couldn't normalize (common on
     * non-TR/non-US numbers or when region detection fails), strip everything
     * except digits and a single leading '+'. Better than dropping the contact.
     */
    private fun fallbackNormalize(raw: String): String? {
        val sb = StringBuilder()
        for ((i, c) in raw.withIndex()) {
            when {
                c == '+' && i == 0 -> sb.append('+')
                c.isDigit() -> sb.append(c)
            }
        }
        val result = sb.toString()
        return result.takeIf { it.length >= 5 }
    }
}
