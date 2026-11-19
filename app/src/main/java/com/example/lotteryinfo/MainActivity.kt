package com.example.lotteryinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
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
import com.example.lotteryinfo.ui.layout.screenSizeClassFromWidth
import com.example.lotteryinfo.ui.screens.HistoryScreen
import com.example.lotteryinfo.ui.screens.HomeScreen
import com.example.lotteryinfo.ui.screens.LiveScreen
import com.example.lotteryinfo.ui.screens.TrendChartScreen
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.round

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
    var selectedTab by remember { mutableIntStateOf(0) }
    var trendLotteryType by remember { mutableStateOf(LotteryType.SSQ) }
    var historyLotteryType by remember { mutableStateOf(LotteryType.SSQ) }
    var isLiveFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lottery_prefs", Context.MODE_PRIVATE) }
    var fontScale by remember {
        mutableFloatStateOf(prefs.getFloat("font_scale", 1.0f).coerceIn(0.85f, 1.65f))
    }
    val updateFontScale: (Float) -> Unit = { delta ->
        val next = (round((fontScale + delta) * 100f) / 100f).coerceIn(0.85f, 1.65f)
        fontScale = next
        prefs.edit().putFloat("font_scale", next).apply()
    }

    val tabs = listOf(
        NavTab("大厅", "开奖大厅", Icons.Default.Home),
        NavTab("直播", "开奖直播", Icons.Default.LiveTv),
        NavTab("走势图", "号码走势图", Icons.Default.ShowChart),
        NavTab("历史", "历史开奖", Icons.Default.History)
    )
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = fontScale)) {
        
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sizeClass = screenSizeClassFromWidth(maxWidth)
        val hideChrome = selectedTab == 1 && isLiveFullscreen

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
                        sizeClass = sizeClass,
                        isFullscreen = isLiveFullscreen,
                        onFullscreenChange = { isLiveFullscreen = it }
                    )
                    2 -> TrendChartScreen(sizeClass = sizeClass, initialType = trendLotteryType)
                    3 -> HistoryScreen(sizeClass = sizeClass, initialType = historyLotteryType)
                }
            }
        }

        Scaffold(topBar = { if (!hideChrome && selectedTab != 1 && selectedTab != 2) { TopAppBar(title = { Text(tabs[selectedTab].title) }, actions = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 8.dp)) { FilledTonalButton(onClick = { updateFontScale(-0.15f) }, enabled = fontScale > 0.86f, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 36.dp)) { Text("字-", fontWeight = FontWeight.Bold) }; FilledTonalButton(onClick = { updateFontScale(0.15f) }, enabled = fontScale < 1.64f, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 36.dp)) { Text("字+", fontWeight = FontWeight.Bold) } } }) } }, bottomBar = { if (!hideChrome && sizeClass == ScreenSizeClass.COMPACT) { NavigationBar { tabs.forEachIndexed { index, tab -> NavigationBarItem(selected = selectedTab == index, onClick = { isLiveFullscreen = false; selectedTab = index }, icon = { Icon(tab.icon, contentDescription = tab.label) }, label = { Text(tab.label) }) } } } }) { innerPadding -> Row(modifier = (if (hideChrome) Modifier else Modifier.padding(innerPadding)).fillMaxSize()) { Box { if (!hideChrome && sizeClass == ScreenSizeClass.EXPANDED) {
                        PermanentDrawerSheet(modifier = Modifier.width(210.dp)) {
                            Text(
                                text = "福彩体彩助手",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
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
                        Spacer(modifier = Modifier.weight(1f))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { updateFontScale(-0.15f) },
                                enabled = fontScale > 0.86f,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("字-", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { updateFontScale(0.15f) },
                                enabled = fontScale < 1.64f,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("字+", fontWeight = FontWeight.Bold)
                            }
                        }
                        } }
               if (!hideChrome && sizeClass == ScreenSizeClass.MEDIUM) {
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
                        } }
                        mainContent(Modifier.weight(1f))
                                   }
        
        }
    }
    }
}
