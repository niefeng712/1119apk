package com.example.lotteryinfo.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class ScreenSizeClass {
    COMPACT,   // < 600dp（手机竖屏）
    MEDIUM,    // 600dp..839dp（9寸平板竖屏 / 小平板）
    EXPANDED   // >= 840dp（9寸–14寸平板横屏 / 大平板）
}

@Composable
fun rememberScreenSizeClass(): ScreenSizeClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> ScreenSizeClass.EXPANDED
        widthDp >= 600 -> ScreenSizeClass.MEDIUM
        else -> ScreenSizeClass.COMPACT
    }
}
