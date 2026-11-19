package com.example.lotteryinfo.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ScreenSizeClass {
    COMPACT,   // < 600dp（手机）
    MEDIUM,    // 600dp..959dp（9寸–10寸平板）
    EXPANDED   // >= 960dp（11寸–14寸大平板横屏）
}

fun screenSizeClassFromWidth(width: Dp): ScreenSizeClass {
    return when {
        width >= 960.dp -> ScreenSizeClass.EXPANDED
        width >= 600.dp -> ScreenSizeClass.MEDIUM
        else -> ScreenSizeClass.COMPACT
    }
}
