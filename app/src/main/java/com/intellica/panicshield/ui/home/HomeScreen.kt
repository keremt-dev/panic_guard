package com.intellica.panicshield.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.intellica.panicshield.ui.AccessibilityStatus
import com.intellica.panicshield.ui.BatteryOptimization

@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var accessibilityEnabled by remember { mutableStateOf(AccessibilityStatus.isEnabled(context)) }
    var batteryOk by remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = AccessibilityStatus.isEnabled(context)
                batteryOk = BatteryOptimization.isIgnoring(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Panic Shield", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (accessibilityEnabled) "Active. Volume up x3 will lock."
            else "Inactive. Grant accessibility access.",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!accessibilityEnabled) {
            Button(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text("Open Accessibility Settings") }
        }
        if (accessibilityEnabled && !batteryOk) {
            Button(onClick = { BatteryOptimization.requestExemption(context) }) {
                Text("Disable battery optimization")
            }
        }
        TextButton(onClick = onOpenSettings) { Text("Settings") }
    }
}
