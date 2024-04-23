package com.practic.usbterminal.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Teal900,
    primaryVariant = Teal900Dark,
    onPrimary = White,
    secondary = GummyDolphins,
    secondaryVariant = OceanBlue,
    onSecondary = Black,
    background = Black,
    onBackground = White,
)

private val LightColorPalette = lightColors(
    primary = Teal900,
    primaryVariant = Teal900Dark,
    onPrimary = White,
    secondary = GummyDolphins,
    secondaryVariant = OceanBlue,
    onSecondary = Black,
    background = White,
    onBackground = Black,
)

@Immutable
data class ExtendedColors(
    val contextualAppBarBackground: Color,
    val contextualAppBarOnBackground: Color,
    val ledColorWhenConnected: Color,
    val ledColorWhenDisconnected: Color,
    val textColorWhenConnected: Color,
    val textColorWhenDisconnected: Color,
    val statusLineBackgroundColor: Color,
    val statusLineTextColor: Color,
    val statusLineDividerColor: Color,
    val textToXmitInputFieldBackgroundColor: Color,
    val textToXmitInputFieldBorderColor: Color,
    val ctrlButtonsLineBackgroundColor: Color,
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        contextualAppBarBackground = ContextualAppBarBackgroundColor,
        contextualAppBarOnBackground = ContextualAppBarOnBackgroundColor,
        ledColorWhenConnected = LedColorWhenConnected,
        ledColorWhenDisconnected = LedColorWhenDisconnected,
        textColorWhenConnected = TextColorWhenConnected,
        textColorWhenDisconnected = TextColorWhenDisconnectedForLightTheme,
        statusLineBackgroundColor = Color.Black,
        statusLineTextColor = Color.LightGray,
        statusLineDividerColor = Color.Transparent,
        textToXmitInputFieldBackgroundColor = Color.Transparent,
        textToXmitInputFieldBorderColor = Color.Transparent,
        ctrlButtonsLineBackgroundColor = Color.DarkGray,
    )
}

object UsbTerminalTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
fun UsbTerminalTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (isDarkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val extendedColors = if (isDarkTheme) {
        ExtendedColors(
            contextualAppBarBackground = ContextualAppBarBackgroundColor,
            contextualAppBarOnBackground = ContextualAppBarOnBackgroundColor,
            ledColorWhenConnected = LedColorWhenConnected,
            ledColorWhenDisconnected = LedColorWhenDisconnected,
            textColorWhenConnected = TextColorWhenConnected,
            textColorWhenDisconnected = TextColorWhenDisconnectedForDarkTheme,
            statusLineBackgroundColor = Color.Black,
            statusLineTextColor = Color.LightGray,
            statusLineDividerColor = Color.Gray,
            textToXmitInputFieldBackgroundColor = TextToXmitInputFieldBackgroundColorForDarkTheme,
            textToXmitInputFieldBorderColor = textToXmitInputFieldBorderColorForDarkTheme,
            ctrlButtonsLineBackgroundColor = Color.DarkGray,
        )
    } else {
        ExtendedColors(
            contextualAppBarBackground = ContextualAppBarBackgroundColor,
            contextualAppBarOnBackground = ContextualAppBarOnBackgroundColor,
            ledColorWhenConnected = LedColorWhenConnected,
            ledColorWhenDisconnected = LedColorWhenDisconnected,
            textColorWhenConnected = TextColorWhenConnected,
            textColorWhenDisconnected = TextColorWhenDisconnectedForLightTheme,
            statusLineBackgroundColor = Color.Black,
            statusLineTextColor = Color.LightGray,
            statusLineDividerColor = Color.Transparent,
            textToXmitInputFieldBackgroundColor = TextToXmitInputFieldBackgroundColorForLightTheme,
            textToXmitInputFieldBorderColor = textToXmitInputFieldBorderColorForLightTheme,
            ctrlButtonsLineBackgroundColor = Color.DarkGray,
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}