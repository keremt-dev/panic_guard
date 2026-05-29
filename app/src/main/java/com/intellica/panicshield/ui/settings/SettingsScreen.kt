package com.intellica.panicshield.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intellica.panicshield.settings.TriggerConfig
import com.intellica.panicshield.ui.photos.CapturedPhotosScreen
import com.intellica.panicshield.ui.protected.ProtectedAppsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    var showPhotos by remember { mutableStateOf(false) }
    if (showPhotos) {
        CapturedPhotosScreen(viewModel = viewModel, onBack = { showPhotos = false })
        return
    }
    var showProtected by remember { mutableStateOf(false) }
    if (showProtected) {
        ProtectedAppsScreen(onBack = { showProtected = false })
        return
    }

    val config by viewModel.config.collectAsState()
    val emergency by viewModel.emergencyContact.collectAsState()
    val captureOn by viewModel.captureOnTrigger.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored; capture silently no-ops without permission */ }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result ignored; user can re-tap "Pick contact" to retry */ }

    val pickContact = rememberLauncherForActivityResult(PickPhoneNumberContract()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = viewModel.saveContactFromUri(context.contentResolver, uri)
            if (!ok) {
                Toast.makeText(
                    context,
                    "Couldn't read that contact's phone number.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            val needed = buildList {
                add(Manifest.permission.SEND_SMS)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionsLauncher.launch(needed)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
        )
    }) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("Trigger", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = config.enabled, onCheckedChange = viewModel::setEnabled)
            }

            Text("Press count: ${config.pressCount}")
            Slider(
                value = config.pressCount.toFloat(),
                onValueChange = { viewModel.setPressCount(it.toInt()) },
                valueRange = TriggerConfig.MIN_PRESS_COUNT.toFloat()..TriggerConfig.MAX_PRESS_COUNT.toFloat(),
                steps = TriggerConfig.MAX_PRESS_COUNT - TriggerConfig.MIN_PRESS_COUNT - 1,
            )

            Text("Window: ${"%.1f".format(config.windowMs / 1000.0)} s")
            Slider(
                value = config.windowMs.toFloat(),
                onValueChange = { viewModel.setWindowMs(it.toLong()) },
                valueRange = TriggerConfig.MIN_WINDOW_MS.toFloat()..TriggerConfig.MAX_WINDOW_MS.toFloat(),
                steps = 5,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibrate on trigger", modifier = Modifier.weight(1f))
                Switch(checked = config.vibrate, onCheckedChange = viewModel::setVibrate)
            }

            HorizontalDivider()

            Text("Emergency contact", style = MaterialTheme.typography.titleMedium)
            Text(
                text = emergency?.let { "${it.displayName}  •  ${it.phoneE164}" }
                    ?: "No contact set. SMS will be skipped on trigger.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { pickContact.launch(Unit) }) {
                    Text(if (emergency == null) "Pick contact" else "Change")
                }
                if (emergency != null) {
                    OutlinedButton(onClick = { viewModel.clearEmergencyContact() }) {
                        Text("Remove")
                    }
                }
            }

            HorizontalDivider()

            Text("Camera", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Capture photo on trigger")
                    Text(
                        "Silently photographs whoever is in front of the phone, stored only on this device.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = captureOn,
                    onCheckedChange = { enabled ->
                        viewModel.setCaptureOnTrigger(enabled)
                        if (enabled) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                )
            }
            OutlinedButton(
                onClick = { showPhotos = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Captured photos")
            }

            HorizontalDivider()

            Text("Protected apps", style = MaterialTheme.typography.titleMedium)
            Text(
                "Apps gated while panic is active — opened ones are kicked back to home.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = { showProtected = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Manage protected apps")
            }
        }
    }
}

private class PickPhoneNumberContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
