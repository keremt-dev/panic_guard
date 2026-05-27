package com.intellica.panicshield.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ShieldRed,
    secondary = ShieldRedDark,
    background = NeutralDark,
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldRed,
    secondary = ShieldRedDark,
    background = NeutralLight,
)

@Composable
fun PanicShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
