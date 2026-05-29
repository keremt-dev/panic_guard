package com.intellica.panicshield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Panic Shield is intentionally always-dark: a security tool reads as more
// serious in a near-black palette, and the state accents (emerald / amber /
// red) carry the meaning rather than the chrome.
private val PanicColorScheme = darkColorScheme(
    primary = AccentDanger,
    onPrimary = TextPrimary,
    secondary = AccentSafe,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgElevated,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextMuted,
    outline = Outline,
    error = AccentDanger,
)

@Composable
fun PanicShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PanicColorScheme,
        typography = Typography,
        content = content,
    )
}
