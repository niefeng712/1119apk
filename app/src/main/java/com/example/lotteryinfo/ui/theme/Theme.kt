package com.example.lotteryinfo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RedBallColor = Color(0xFFD93025)
val BlueBallColor = Color(0xFF1A73E8)
val GoldAccent = Color(0xFFE37400)

private val AppColorScheme = lightColorScheme(
    primary = Color(0xFFC5221F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE8E6),
    onPrimaryContainer = Color(0xFF8C1D18),
    secondary = Color(0xFF1A73E8),
    onSecondary = Color.White,
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    onSurface = Color(0xFF1F2329),
    surfaceVariant = Color(0xFFEDF0F5)
)

@Composable
fun LotteryInfoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
