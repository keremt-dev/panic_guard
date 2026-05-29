package com.intellica.panicshield.panic

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.panicStore by preferencesDataStore(name = "panic_state")

class PanicStateRepository(private val context: Context) {

    val state: Flow<PanicState> = context.panicStore.data.map { prefs ->
        if (prefs[ACTIVE] == true) PanicState.ACTIVE else PanicState.IDLE
    }

    val activatedAt: Flow<Long?> = context.panicStore.data.map { it[ACTIVATED_AT] }

    suspend fun activate(now: Long) {
        context.panicStore.edit {
            it[ACTIVE] = true
            it[ACTIVATED_AT] = now
        }
    }

    suspend fun clear() {
        context.panicStore.edit {
            it[ACTIVE] = false
            it.remove(ACTIVATED_AT)
        }
    }

    private companion object {
        val ACTIVE = booleanPreferencesKey("active")
        val ACTIVATED_AT = longPreferencesKey("activated_at")
    }
}
