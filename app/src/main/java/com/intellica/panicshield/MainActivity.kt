package com.intellica.panicshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.intellica.panicshield.settings.SettingsRepository
import com.intellica.panicshield.ui.home.HomeScreen
import com.intellica.panicshield.ui.onboarding.OnboardingScreen
import com.intellica.panicshield.ui.settings.SettingsScreen
import com.intellica.panicshield.ui.theme.PanicShieldTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = SettingsRepository(applicationContext)
        setContent {
            PanicShieldTheme {
                var stage by remember { mutableStateOf<Stage?>(null) }
                LaunchedEffect(Unit) {
                    stage = if (repo.onboardingDone.first()) Stage.Home else Stage.Onboarding
                }
                when (stage) {
                    Stage.Onboarding -> OnboardingScreen(onDone = {
                        lifecycleScope.launch {
                            repo.markOnboardingDone()
                            stage = Stage.Home
                        }
                    })
                    Stage.Home -> {
                        var showSettings by remember { mutableStateOf(false) }
                        if (showSettings) SettingsScreen(onBack = { showSettings = false })
                        else HomeScreen(onOpenSettings = { showSettings = true })
                    }
                    null -> Unit
                }
            }
        }
    }

    private enum class Stage { Onboarding, Home }
}
