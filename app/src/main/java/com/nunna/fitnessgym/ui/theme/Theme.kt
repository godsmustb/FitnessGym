package com.nunna.fitnessgym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Design tokens. Dark-first; orange = primary muscles, amber = helpers (same as the figure). */
object Tokens {
    val Bg = Color(0xFF10131A)
    val Surface = Color(0xFF181C25)
    val SurfaceHi = Color(0xFF222837)
    val Text = Color(0xFFE8EBF2)
    val TextDim = Color(0xFF9AA3B5)
    val Prime = Color(0xFFFF4528)
    val Synergist = Color(0xFFFFB020)
    val Accent = Color(0xFF3FA7D6)
    val Mat = Color(0xFF2C6E6A)
}

private val scheme = darkColorScheme(
    primary = Tokens.Prime,
    onPrimary = Color.White,
    secondary = Tokens.Synergist,
    tertiary = Tokens.Accent,
    background = Tokens.Bg,
    onBackground = Tokens.Text,
    surface = Tokens.Surface,
    onSurface = Tokens.Text,
    surfaceVariant = Tokens.SurfaceHi,
    onSurfaceVariant = Tokens.TextDim,
    surfaceContainer = Tokens.Surface,
    surfaceContainerHigh = Tokens.SurfaceHi,
)

@Composable
fun FitnessGymTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)
