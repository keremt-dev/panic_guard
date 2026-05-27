package com.intellica.panicshield.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intellica.panicshield.settings.TriggerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val config by viewModel.config.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
        )
    }) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
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
        }
    }
}
