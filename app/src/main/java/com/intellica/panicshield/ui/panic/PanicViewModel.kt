package com.intellica.panicshield.ui.panic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.intellica.panicshield.panic.PanicState
import com.intellica.panicshield.panic.PanicStateRepository
import com.intellica.panicshield.panic.SafePin
import com.intellica.panicshield.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanicViewModel(application: Application) : AndroidViewModel(application) {

    private val stateRepo = PanicStateRepository(application)
    private val settingsRepo = SettingsRepository(application)

    val isPanicActive: StateFlow<Boolean> = stateRepo.state
        .map { it == PanicState.ACTIVE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** True once a safe PIN has been configured. */
    val hasSafePin: StateFlow<Boolean> = settingsRepo.safePinHash
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Attempt to disarm. If a safe PIN is set, [pin] must match. If no PIN is
     * set, any call disarms (the user is prompted to set one). Returns true on
     * success.
     */
    suspend fun disarm(pin: String): Boolean {
        val stored = settingsRepo.safePinHash.first()
        val ok = stored == null || withContext(Dispatchers.Default) { SafePin.verify(pin, stored) }
        if (ok) stateRepo.clear()
        return ok
    }

    fun setSafePin(pin: String) {
        viewModelScope.launch {
            val hash = withContext(Dispatchers.Default) { SafePin.hash(pin) }
            settingsRepo.setSafePinHash(hash)
        }
    }

    fun clearSafePin() {
        viewModelScope.launch { settingsRepo.setSafePinHash(null) }
    }
}
