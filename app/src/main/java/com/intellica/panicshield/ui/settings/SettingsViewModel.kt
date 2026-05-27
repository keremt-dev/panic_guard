package com.intellica.panicshield.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.settings.TriggerConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val config: StateFlow<TriggerConfig> = repo.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TriggerConfig.DEFAULT,
    )

    fun setEnabled(value: Boolean) = update { it.copy(enabled = value) }
    fun setPressCount(value: Int) = update { it.copy(pressCount = value) }
    fun setWindowMs(value: Long) = update { it.copy(windowMs = value) }
    fun setVibrate(value: Boolean) = update { it.copy(vibrate = value) }

    private fun update(transform: (TriggerConfig) -> TriggerConfig) {
        viewModelScope.launch { repo.update(transform) }
    }
}
