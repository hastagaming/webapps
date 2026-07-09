package com.web.apps.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.web.apps.core.preferences.AppThemeMode

private val DefaultLightColors = lightColorScheme(
    primary = Color(0xFF2196F3)
)

private val DefaultDarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9)
)

@Composable
fun WebAppsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColorHex: String? = null,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        accentColorHex != null -> {
            val accent = try {
                Color(android.graphics.Color.parseColor(accentColorHex))
            } catch (e: Exception) {
                if (darkTheme) Color(0xFF90CAF9) else Color(0xFF2196F3)
            }
            if (darkTheme) darkColorScheme(primary = accent) else lightColorScheme(primary = accent)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DefaultDarkColors
        else -> DefaultLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}