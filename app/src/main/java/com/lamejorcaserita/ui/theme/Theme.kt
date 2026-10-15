package com.lamejorcaserita.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ColorSchemeLight = lightColorScheme(
    // Primarios (verde)
    primary = VerdeHierba,
    onPrimary = TextoSobreVerde,
    primaryContainer = VerdeMenta,
    onPrimaryContainer = VerdeSombra,

    // Secundarios (dorado)
    secondary = DoradoBrillante,
    onSecondary = TextoPrincipal,
    secondaryContainer = DoradoSuave,
    onSecondaryContainer = DoradoOscuro,

    // Terciarios (rojo vino)
    tertiary = RojoVino,
    onTertiary = TextoSobreVerde,
    tertiaryContainer = RojoVinoClaro,
    onTertiaryContainer = RojoVino,

    // Fondos
    background = BlancoCalido,
    onBackground = TextoPrincipal,
    surface = BlancoPuro,
    onSurface = TextoPrincipal,
    surfaceVariant = Crema,
    onSurfaceVariant = TextoSecundario,

    // Bordes y errores
    outline = GrisNeutro,
    outlineVariant = GrisNeutro,
    error = RojoError,
    onError = TextoSobreVerde,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun LaMejorCaseritaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = ColorSchemeLight

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VerdeHierba.toArgb()
            window.navigationBarColor = BlancoPuro.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}