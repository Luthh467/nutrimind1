package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NutriGreenMedium,
    secondary = NutriTealPrimary,
    tertiary = NutriAmber,
    background = NutriSlate900,
    surface = NutriSlate800,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = NutriSlate900,
    onBackground = NutriSlate50,
    onSurface = NutriSlate50
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NutriGreenPrimary,
    secondary = NutriTealDark,
    tertiary = NutriAmber,
    background = NutriSlate50,
    surface = Color.White,
    primaryContainer = NutriGreenLight,
    onPrimaryContainer = NutriGreenDark,
    secondaryContainer = NutriTealLight,
    onSecondaryContainer = NutriTealDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NutriSlate900,
    onSurface = NutriSlate800
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Avoid dynamic color to keep branded nutrition theme consistent
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

