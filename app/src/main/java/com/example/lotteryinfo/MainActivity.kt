package com.example.lotteryinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.material3.TextButton
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
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.ui.layout.screenSizeClassFromWidth
import com.example.lotteryinfo.ui.screens.HistoryScreen
import com.example.lotteryinfo.ui.screens.HomeScreen
import com.example.lotteryinfo.ui.screens.LiveScreen
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
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.lotteryinfo.update.AppUpdater
import com.example.lotteryinfo.update.UpdateInfo
import kotlinx.coroutines.launch

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
    var historyLotteryType by remember { mutableStateOf(LotteryType.FC3D) }
    var liveTargetOrg by remember { mutableStateOf(LotteryOrg.FC) }
    var isLiveFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lottery_prefs", Context.MODE_PRIVATE) }
    var fontScale by remember {
        mutableFloatStateOf(prefs.getFloat("font_scale", 1.12f).coerceIn(0.95f, 1.30f))
    }
    val updateFontScale: (Float) -> Unit = { delta ->
        val next = (round((fontScale + delta) * 100f) / 100f).coerceIn(0.95f, 1.30f)
        fontScale = next
        prefs.edit().putFloat("font_scale", next).apply()
    }
    val coroutineScope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { updateInfo = AppUpdater.checkUpdate(context) }
    val checkUpdateManually: () -> Unit = { coroutineScope.launch { val info = AppUpdater.checkUpdate(context); if (info != null) { updateInfo = info } else { Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show() } } }
    
    val tabs = listOf(
        NavTab("大厅", "开奖大厅", Icons.Default.Home),
        NavTab("直播", "开奖直播", Icons.Default.LiveTv),
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
                            selectedTab = 2
                        },
                        onOpenLive = { org -> liveTargetOrg = org; selectedTab = 1 }
                    )
                    1 -> LiveScreen(
                        sizeClass = sizeClass,
                       initialOrg = liveTargetOrg,
                        isFullscreen = isLiveFullscreen,
                        onFullscreenChange = { isLiveFullscreen = it }
                    )
                    2 -> HistoryScreen(sizeClass = sizeClass, initialType = historyLotteryType)
                }
            }
        }

        Scaffold(topBar = { if (!hideChrome && selectedTab != 1) { TopAppBar(title = { Text(tabs[selectedTab].title) }, actions = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 8.dp)) { FilledTonalButton(onClick = { updateFontScale(-0.07f) }, enabled = fontScale > 0.96f, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 36.dp)) { Text("字-", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }; FilledTonalButton(onClick = { updateFontScale(0.07f) }, enabled = fontScale < 1.29f, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 36.dp)) { Text("字+", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) } } }) } }, bottomBar = { if (!hideChrome && sizeClass == ScreenSizeClass.COMPACT) { NavigationBar { tabs.forEachIndexed { index, tab -> NavigationBarItem(selected = selectedTab == index, onClick = { isLiveFullscreen = false; selectedTab = index }, icon = { Icon(tab.icon, contentDescription = tab.label) }, label = { Text(tab.label) }) } } } }) { innerPadding -> Row(modifier = (if (hideChrome) Modifier else Modifier.padding(innerPadding)).fillMaxSize()) { Box { if (!hideChrome && sizeClass == ScreenSizeClass.EXPANDED) {
                        PermanentDrawerSheet(modifier = Modifier.width(232.dp)) {
                            Text(
                                text = "福彩体彩助手", maxLines = 1, softWrap = false,
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
                        NavigationDrawerItem(selected = false, onClick = checkUpdateManually, icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "检查更新") }, label = { Text("检查更新", maxLines = 1, softWrap = false) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
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
                                onClick = { updateFontScale(-0.07f) },
                                enabled = fontScale > 0.96f,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("字-", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                            FilledTonalButton(
                                onClick = { updateFontScale(0.07f) },
                                enabled = fontScale < 1.29f,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("字+", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
                        NavigationRailItem(selected = false, onClick = checkUpdateManually, icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "检查更新") }, label = { Text("检查更新", maxLines = 1, softWrap = false) })
                            }
                        } }
                        mainContent(Modifier.weight(1f))
                                   }
        
        }
    }
    }
    val currentUpdate = updateInfo
    if (currentUpdate != null) {
        AlertDialog(
            onDismissRequest = { if (!isDownloading) updateInfo = null },
            title = { Text("发现新版本 v${currentUpdate.latestVersion}") },
            text = {
                Column {
                    if (currentUpdate.releaseNotes.isNotBlank()) {
                        Text(currentUpdate.releaseNotes)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (isDownloading) {
                        Text("正在下载更新: $downloadProgress%")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDownloading,
                    onClick = {
                        isDownloading = true
                        downloadProgress = 0
                        coroutineScope.launch {
                            val success = AppUpdater.downloadAndInstallApk(
                                context = context,
                                downloadUrl = currentUpdate.downloadUrl,
                                onProgress = { progress -> downloadProgress = progress }
                            )
                            isDownloading = false
                            if (success) {
                                updateInfo = null
                            } else {
                                Toast.makeText(context, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(if (isDownloading) "下载中..." else "立即更新")
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { updateInfo = null }) {
                        Text("稍后再说")
                    }
                }
            }
        )
    }
}
