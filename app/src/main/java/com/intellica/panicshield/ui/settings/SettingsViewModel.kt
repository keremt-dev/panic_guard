package com.intellica.panicshield.ui.settings

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.intellica.panicshield.camera.CaptureStorage
import com.intellica.panicshield.camera.CapturedPhoto
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.settings.TriggerConfig
import com.intellica.panicshield.sms.ContactResolver
import com.intellica.panicshield.sms.EmergencyContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val config: StateFlow<TriggerConfig> = repo.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TriggerConfig.DEFAULT,
    )

    val emergencyContact: StateFlow<EmergencyContact?> = repo.emergencyContact.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val captureOnTrigger: StateFlow<Boolean> = repo.captureOnTrigger.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    private val filesDir = application.filesDir
    private val _photos = MutableStateFlow<List<CapturedPhoto>>(emptyList())
    val photos: StateFlow<List<CapturedPhoto>> = _photos.asStateFlow()

    fun setCaptureOnTrigger(value: Boolean) {
        viewModelScope.launch { repo.setCaptureOnTrigger(value) }
    }

    fun refreshPhotos() {
        viewModelScope.launch {
            _photos.value = withContext(Dispatchers.IO) { CaptureStorage.list(filesDir) }
        }
    }

    fun deletePhoto(photo: CapturedPhoto) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { photo.file.delete() }
            refreshPhotos()
        }
    }

    fun setEnabled(value: Boolean) = update { it.copy(enabled = value) }
    fun setPressCount(value: Int) = update { it.copy(pressCount = value) }
    fun setWindowMs(value: Long) = update { it.copy(windowMs = value) }
    fun setVibrate(value: Boolean) = update { it.copy(vibrate = value) }

    fun clearEmergencyContact() {
        viewModelScope.launch { repo.setEmergencyContact(null) }
    }

    /**
     * Resolve the picker-returned URI on IO, normalize phone to E.164, and
     * persist. Returns `true` if the contact was saved, `false` if the URI
     * couldn't be resolved (no phone number, blocked URI, etc.).
     */
    suspend fun saveContactFromUri(resolver: ContentResolver, uri: Uri): Boolean {
        val resolved = withContext(Dispatchers.IO) {
            ContactResolver.resolve(resolver, uri)
        } ?: return false
        repo.setEmergencyContact(resolved)
        return true
    }

    private fun update(transform: (TriggerConfig) -> TriggerConfig) {
        viewModelScope.launch { repo.update(transform) }
    }
}
