package com.intellica.panicshield.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.intellica.panicshield.sms.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "panic_shield_settings")

class SettingsRepository(private val context: Context) {

    val config: Flow<TriggerConfig> = context.dataStore.data.map { prefs ->
        TriggerConfig(
            enabled = prefs[ENABLED] ?: TriggerConfig.DEFAULT.enabled,
            pressCount = prefs[PRESS_COUNT] ?: TriggerConfig.DEFAULT.pressCount,
            windowMs = prefs[WINDOW_MS] ?: TriggerConfig.DEFAULT.windowMs,
            vibrate = prefs[VIBRATE] ?: TriggerConfig.DEFAULT.vibrate,
        ).normalized()
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    val emergencyContact: Flow<EmergencyContact?> = context.dataStore.data.map { prefs ->
        val name = prefs[EMERGENCY_CONTACT_NAME]
        val phone = prefs[EMERGENCY_CONTACT_E164]
        if (name != null && phone != null) EmergencyContact(name, phone) else null
    }

    suspend fun update(transform: (TriggerConfig) -> TriggerConfig) {
        context.dataStore.edit { prefs ->
            val current = TriggerConfig(
                enabled = prefs[ENABLED] ?: TriggerConfig.DEFAULT.enabled,
                pressCount = prefs[PRESS_COUNT] ?: TriggerConfig.DEFAULT.pressCount,
                windowMs = prefs[WINDOW_MS] ?: TriggerConfig.DEFAULT.windowMs,
                vibrate = prefs[VIBRATE] ?: TriggerConfig.DEFAULT.vibrate,
            )
            val next = transform(current).normalized()
            prefs[ENABLED] = next.enabled
            prefs[PRESS_COUNT] = next.pressCount
            prefs[WINDOW_MS] = next.windowMs
            prefs[VIBRATE] = next.vibrate
        }
    }

    suspend fun markOnboardingDone() {
        context.dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun setEmergencyContact(contact: EmergencyContact?) {
        context.dataStore.edit { prefs ->
            if (contact == null) {
                prefs.remove(EMERGENCY_CONTACT_NAME)
                prefs.remove(EMERGENCY_CONTACT_E164)
            } else {
                prefs[EMERGENCY_CONTACT_NAME] = contact.displayName
                prefs[EMERGENCY_CONTACT_E164] = contact.phoneE164
            }
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val PRESS_COUNT = intPreferencesKey("press_count")
        val WINDOW_MS = longPreferencesKey("window_ms")
        val VIBRATE = booleanPreferencesKey("vibrate")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val EMERGENCY_CONTACT_NAME = stringPreferencesKey("emergency_contact_name")
        val EMERGENCY_CONTACT_E164 = stringPreferencesKey("emergency_contact_e164")
    }
}
