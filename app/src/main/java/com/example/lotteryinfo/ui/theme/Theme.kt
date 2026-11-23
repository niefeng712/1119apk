package com.example.lotteryinfo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val RedBallColor = Color(0xFFE53935)
val BlueBallColor = Color(0xFF1E88E5)
val GoldAccent = Color(0xFFE37400)

/** 福彩 / 体彩 品牌色 */
val FcBrandColor = Color(0xFFC62828)
val TcBrandColor = Color(0xFF1565C0)

val SubtleText = Color(0xFF5F6368)
val SuccessGreen = Color(0xFF1E8E3E)
val MutedGray = Color(0xFF9AA0A6)

private val AppColorScheme = lightColorScheme(
    primary = Color(0xFFC5221F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE8E6),
    onPrimaryContainer = Color(0xFF8C1D18),
    secondary = Color(0xFF1A73E8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EEFD),
    onSecondaryContainer = Color(0xFF0B3D91),
    background = Color(0xFFF5F6FA),
    surface = Color.White,
    onSurface = Color(0xFF111315),
    surfaceVariant = Color(0xFFEDF0F5),
    outlineVariant = Color(0xFFE1E4EA)
)

// 适老化大号加粗全局字体排版
private val SeniorTypography = Typography(
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleMedium = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
    labelMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
)

@Composable
fun LotteryInfoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = SeniorTypography,
        content = content
    )
}
