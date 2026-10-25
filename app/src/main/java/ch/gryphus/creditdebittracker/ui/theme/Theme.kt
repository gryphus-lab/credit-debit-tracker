package ch.gryphus.creditdebittracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ch.gryphus.creditdebittracker.data.ThemeMode

// High-contrast, flat palette in the spirit of the CallSimulator app:
// near-black surfaces, a confident green for "owed to you" (credit) and a clear
// red for "you owe" (debit).
private val Green = Color(0xFF4CAF50)
private val Red = Color(0xFFF44336)
private val Ink = Color(0xFF121212)
private val Surface = Color(0xFF1E1E1E)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Color.Black,
    secondary = Red,
    background = Ink,
    surface = Surface,
    onSurface = Color.White,
    error = Red,
)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.Black,
    secondary = Red,
    error = Red,
)

@Composable
fun CreditDebitTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
