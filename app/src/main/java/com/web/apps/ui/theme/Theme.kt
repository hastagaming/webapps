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
import com.web.apps.core.plugin.PluginManifest
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
    activeThemePlugin: PluginManifest? = null,
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

    fun parseColor(hex: String, fallback: Color): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }

    val colorScheme = when {
        activeThemePlugin?.colors != null -> {
            val c = activeThemePlugin.colors
            darkColorScheme(
                primary = parseColor(c.primary, Color(0xFF90CAF9)),
                background = parseColor(c.background, Color(0xFF121212)),
                surface = parseColor(c.surface, Color(0xFF1E1E1E)),
                surfaceVariant = parseColor(c.surfaceVariant, Color(0xFF2D2D2D)),
                onSurface = parseColor(c.onSurface, Color.White),
                onPrimary = parseColor(c.onPrimary, Color.Black),
                error = parseColor(c.error, Color(0xFFCF6679))
            )
        }
        accentColorHex != null -> {
            val accent = parseColor(accentColorHex, if (darkTheme) Color(0xFF90CAF9) else Color(0xFF2196F3))
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