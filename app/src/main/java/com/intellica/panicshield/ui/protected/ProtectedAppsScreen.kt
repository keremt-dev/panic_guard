package com.intellica.panicshield.ui.protected

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedAppsScreen(
    viewModel: ProtectedAppsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val apps by viewModel.apps.collectAsState()
    val protectedPackages by viewModel.protectedPackages.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadApps() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Protected apps") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
        )
    }) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            Text(
                "While panic is active, these apps are kicked back to the home screen when opened.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Switch(
                            checked = app.packageName in protectedPackages,
                            onCheckedChange = { viewModel.setProtected(app.packageName, it) },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
