package com.example.lotteryinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.ui.layout.rememberScreenSizeClass
import com.example.lotteryinfo.ui.screens.HistoryScreen
import com.example.lotteryinfo.ui.screens.HomeScreen
import com.example.lotteryinfo.ui.screens.LiveScreen
import com.example.lotteryinfo.ui.screens.TrendChartScreen
import com.example.lotteryinfo.ui.theme.LotteryInfoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LotteryInfoTheme {
                LotteryMainApp()
            }
        }
    }
}

private data class NavTab(val label: String, val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryMainApp() {
    val sizeClass = rememberScreenSizeClass()
    var selectedTab by remember { mutableIntStateOf(0) }
    var trendLotteryType by remember { mutableStateOf(LotteryType.SSQ) }
    var historyLotteryType by remember { mutableStateOf(LotteryType.SSQ) }
    var isLiveFullscreen by remember { mutableStateOf(false) }

    val tabs = listOf(
        NavTab("大厅", "开奖大厅", Icons.Default.Home),
        NavTab("直播", "开奖直播", Icons.Default.LiveTv),
        NavTab("走势图", "号码走势图", Icons.Default.ShowChart),
        NavTab("历史", "历史开奖", Icons.Default.History)
    )

    val mainContent: @Composable (Modifier) -> Unit = { mod ->
        Box(modifier = mod.fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeScreen(
                    sizeClass = sizeClass,
                    onOpenHistory = { type ->
                        historyLotteryType = type
                        selectedTab = 3
                    },
                    onOpenTrend = { type ->
                        trendLotteryType = type
                        selectedTab = 2
                    },
                    onOpenLive = { selectedTab = 1 }
                )
                1 -> LiveScreen(
                    isFullscreen = isLiveFullscreen,
                    onFullscreenChange = { isLiveFullscreen = it }
                )
                2 -> TrendChartScreen(sizeClass = sizeClass, initialType = trendLotteryType)
                3 -> HistoryScreen(sizeClass = sizeClass, initialType = historyLotteryType)
            }
        }
    }

    // 关键修复：全屏时在同一布局树层级隐藏外围导航框架，不销毁/重建 LiveScreen 内的 WebView
    if (selectedTab == 1 && isLiveFullscreen) {
        mainContent(Modifier.fillMaxSize())
        return
    }

    when (sizeClass) {
        ScreenSizeClass.EXPANDED -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(200.dp)) {
                        Text(
                            text = "福彩体彩助手",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(20.dp)
                        )
                        tabs.forEachIndexed { index, tab ->
                            NavigationDrawerItem(
                                selected = selectedTab == index,
                                onClick = {
                                    isLiveFullscreen = false
                                    selectedTab = index
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.title) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        if (selectedTab != 1) {
                            TopAppBar(title = { Text(tabs[selectedTab].title) })
                        }
                    }
                ) { innerPadding ->
                    mainContent(Modifier.padding(innerPadding))
                }
            }
        }
        ScreenSizeClass.MEDIUM -> {
            Scaffold(
                topBar = {
                    if (selectedTab != 1) {
                        TopAppBar(title = { Text(tabs[selectedTab].title) })
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    NavigationRail {
                        tabs.forEachIndexed { index, tab ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = {
                                    isLiveFullscreen = false
                                    selectedTab = index
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                    mainContent(Modifier.weight(1f))
                }
            }
        }
        ScreenSizeClass.COMPACT -> {
            Scaffold(
                topBar = {
                    if (selectedTab != 1) {
                        TopAppBar(title = { Text(tabs[selectedTab].title) })
                    }
                },
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    isLiveFullscreen = false
                                    selectedTab = index
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                mainContent(Modifier.padding(innerPadding))
            }
        }
    }
}
