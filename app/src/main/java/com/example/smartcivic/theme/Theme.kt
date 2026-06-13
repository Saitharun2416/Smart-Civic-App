package com.example.smartcivic.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// Global application settings for UI-driven features like theme toggles
object AppSettings {
  var isDarkMode = mutableStateOf(false)
  var notificationsEnabled = mutableStateOf(true)
  var offlineSyncEnabled = mutableStateOf(false)
}

private val DarkColorScheme = darkColorScheme(
  primary = ChimeGreen,
  secondary = ChimeGreen,
  tertiary = ChimeLightGreenBg,
  background = Color(0xFF092015), // Deep green-black background
  surface = Color(0xFF0D3220),    // Deep green-gray surface
  onPrimary = Color(0xFF092015),
  onSecondary = Color(0xFF092015),
  onTertiary = ChimeDarkGreen,
  onBackground = Slate50,
  onSurface = Slate50,
  onSurfaceVariant = Slate400,
  outline = Slate700,
  error = Rose500
)

private val LightColorScheme = lightColorScheme(
  primary = ChimeForestGreen,
  secondary = ChimeGreen,
  tertiary = ChimeLightGreenBg,
  background = Slate50,
  surface = Color.White,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = ChimeDarkGreen,
  onBackground = Slate900,
  onSurface = Slate900,
  onSurfaceVariant = Slate600,
  outline = Slate300,
  error = Rose500
)

@Composable
fun SmartCivicTheme(
  darkTheme: Boolean = AppSettings.isDarkMode.value,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
