package com.abuomar.sawti.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.abuomar.sawti.core.Currency

/* ============================== schemes ================================== */

private val LightColors = lightColorScheme(
    primary = Brand600,
    onPrimary = Color.White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand800,
    secondary = Brand500,
    onSecondary = Color.White,
    secondaryContainer = Brand100,
    onSecondaryContainer = Brand800,
    tertiary = Accent,
    onTertiary = Color.White,
    tertiaryContainer = AccentSoft,
    onTertiaryContainer = Color(0xFF7C4A03),
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Color(0xFF7F1D1D),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutlineVariant,
    outlineVariant = LightOutline,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightSurface,
)

private val DarkColors = darkColorScheme(
    primary = Brand400,
    onPrimary = Brand900,
    primaryContainer = Brand800,
    onPrimaryContainer = Brand100,
    secondary = Brand400,
    onSecondary = Brand900,
    secondaryContainer = DarkBrand100,
    onSecondaryContainer = Brand300,
    tertiary = Accent,
    onTertiary = Color(0xFF3B2400),
    tertiaryContainer = Color(0xFF5A3A05),
    onTertiaryContainer = AccentSoft,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutlineVariant,
    outlineVariant = DarkOutline,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurface,
)

val SawtiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/* ====================== لغة/وحدة عرض عبر CompositionLocal ================== */

data class SawtiLocals(
    val arabic: Boolean = true,
    val displayUnit: Currency.Unit = Currency.Unit.NEW,
)

val LocalSawti = staticCompositionLocalOf { SawtiLocals() }

/** هل الواجهة عربية؟ */
val isArabicUi: Boolean
    @Composable get() = LocalSawti.current.arabic

/** تنسيق مبلغ حسب لغة الواجهة ووحدة العرض المختارة */
@Composable
fun rememberMoneyFormatter(): (Long) -> String {
    val locals = LocalSawti.current
    return { value -> Currency.format(value, locals.displayUnit, locals.arabic) }
}

@Composable
fun rememberPlainAmount(): (Long) -> Long {
    val unit = LocalSawti.current.displayUnit
    return { value -> if (unit == Currency.Unit.LEGACY) Currency.newToLegacy(value) else value }
}

/* ================================= Theme ================================= */

@Composable
fun SawtiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,      // نثبت هوية التطبيق بدل ألوان Material You
    arabic: Boolean = true,
    displayUnit: Currency.Unit = Currency.Unit.NEW,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context)
            else androidx.compose.material3.dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            window.statusBarColor = if (darkTheme) DarkBackground.toArgbInt() else Brand700.toArgbInt()
        }
    }

    CompositionLocalProvider(LocalSawti provides SawtiLocals(arabic, displayUnit)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SawtiTypography,
            shapes = SawtiShapes,
            content = content,
        )
    }
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
