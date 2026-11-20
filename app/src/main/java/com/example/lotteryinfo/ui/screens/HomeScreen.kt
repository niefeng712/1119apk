package com.example.lotteryinfo.ui.screens
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lotteryinfo.data.LotteryRepository
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.ui.components.LotteryBallRow
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.ui.theme.GoldAccent
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    sizeClass: ScreenSizeClass,
    onOpenHistory: (LotteryType) -> Unit,
    onOpenLive: (LotteryOrg) -> Unit
) {
    var selectedOrg by remember { mutableStateOf(LotteryOrg.ALL) }
    var draws by remember { mutableStateOf<List<LotteryDraw>>(emptyList()) }
    var statusText by remember { mutableStateOf("正在同步全部彩种最新开奖...") }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var sinaSyncTrigger by remember { mutableIntStateOf(1) }
    var lastSyncTime by remember { mutableStateOf("--:--:--") }
    fun formatBeijingTime(): String = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }.format(Date())
    LaunchedEffect(refreshTrigger) {
      if (refreshTrigger > 0) sinaSyncTrigger++
        isLoading = true
        val res = LotteryRepository.fetchLatestAll()
        draws = res.draws
        statusText = res.statusMessage
        isLoading = false
lastSyncTime = formatBeijingTime()
    }
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
            val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val intervalMs = if (minutes in (21 * 60 + 14)..(21 * 60 + 40)) 8_000L else 60_000L    
            delay(intervalMs)
            if (minutes in (21 * 60 + 14)..(21 * 60 + 40)) sinaSyncTrigger++
            val res = LotteryRepository.fetchLatestAll()
            draws = res.draws
            statusText = res.statusMessage
lastSyncTime = formatBeijingTime()
        }
    }
    
    val filteredDraws = remember(selectedOrg, draws) {
        if (selectedOrg == LotteryOrg.ALL) draws
        else draws.filter { it.lotteryType.org == selectedOrg }
    }

    val columns = when (sizeClass) {
        ScreenSizeClass.COMPACT -> 1
        ScreenSizeClass.MEDIUM -> 2
        ScreenSizeClass.EXPANDED -> 3
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(columns) }) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LotteryOrg.entries.forEach { org ->
                            FilterChip(
                                selected = selectedOrg == org,
                                onClick = { selectedOrg = org },
                                label = { Text(org.label) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                        text = "$statusText · 最近更新 $lastSyncTime",    
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = { refreshTrigger++ }) {
                            Text("刷新")
                        }
                    }
                }
            }

            items(filteredDraws) { draw ->
                LatestDrawCard(
                    draw = draw,
lastSyncTime = lastSyncTime,
                    onOpenHistory = { onOpenHistory(draw.lotteryType) },
                    onOpenLive ={  onOpenLive(draw.lotteryType.org) }
                )
            }
        }
    }
  AndroidView(modifier = Modifier.size(1.dp).alpha(0f), factory = { ctx -> WebView(ctx).apply { settings.javaScriptEnabled = true; webViewClient = object : WebViewClient() { override fun onPageFinished(view: WebView?, url: String?) { super.onPageFinished(view, url); view?.evaluateJavascript("(function(){var t=document.body.innerText||'';var r=[];var m1=t.match(/福彩3D[\\s\\S]{0,120}?(20\\d{5}|\\d{5,7})期[\\s\\S]{0,80}?(\\d[\\s,]+\\d[\\s,]+\\d)/);if(m1)r.push('FC3D:'+m1[1]+'|'+m1[2]);var m2=t.match(/排列[3三][\\s\\S]{0,120}?(20\\d{5}|\\d{5,7})期[\\s\\S]{0,80}?(\\d[\\s,]+\\d[\\s,]+\\d)/);if(m2)r.push('PL3:'+m2[1]+'|'+m2[2]);var m3=t.match(/排列[5五][\\s\\S]{0,120}?(20\\d{5}|\\d{5,7})期[\\s\\S]{0,80}?(\\d[\\s,]+\\d[\\s,]+\\d[\\s,]+\\d[\\s,]+\\d)/);if(m3)r.push('PL5:'+m3[1]+'|'+m3[2]);return r.join(';');})()") { raw -> val s = raw?.trim('"') ?: ""; if (s.isNotBlank() && s != "null") { val tm = mapOf("FC3D" to (LotteryType.FC3D to 3), "PL3" to (LotteryType.PL3 to 3), "PL5" to (LotteryType.PL5 to 5)); var ndList = draws; var changed = false; s.split(";").forEach { item -> val idx = item.indexOf(':'); if (idx > 0) { val info = tm[item.substring(0, idx)]; val parts = item.substring(idx + 1).split("|"); if (info != null && parts.size == 2) { val issue = parts[0].trim(); val nums = parts[1].trim().split(Regex("[,\\s]+")).mapNotNull { it.toIntOrNull() }; if (issue.isNotBlank() && nums.size == info.second) { val cur = ndList.firstOrNull { it.lotteryType == info.first }?.issue ?: ""; if (LotteryRepository.normalizeIssue(issue) > LotteryRepository.normalizeIssue(cur)) { val nd = LotteryDraw(info.first, issue, "今日极速", nums, "", "新浪极速", "", ""); LotteryRepository.updateFastLatest(nd); ndList = ndList.map { if (it.lotteryType == info.first) nd else it }; changed = true } } } } }; if (changed) { draws = ndList; statusText = "极速开奖同步成功" }; lastSyncTime = formatBeijingTime() } } } }; loadUrl("https://lotto.sina.cn/open") } }, update = { w -> if (w.tag != sinaSyncTrigger) { w.tag = sinaSyncTrigger; w.loadUrl("https://lotto.sina.cn/open") } })  
}

@Composable
private fun LatestDrawCard(
    draw: LotteryDraw,
lastSyncTime: String,
    onOpenHistory: () -> Unit,
    onOpenLive: () -> Unit
) {
    val twoDigits = !draw.lotteryType.isDigitGame
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draw.lotteryType.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "第 ${draw.issue} 期 · ${draw.drawDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5F6368)
                    )
Text(text = "更新于 $lastSyncTime", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5F6368))
                }
                Text(
                    text = draw.poolAmount,
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            LotteryBallRow(
                redBalls = draw.redBalls,
                blueBalls = draw.blueBalls,
                padTwoDigits = twoDigits
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${draw.lotteryType.org.label} · ${draw.lotteryType.drawSchedule}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F6368)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Text("历史开奖", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenLive,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC5221F)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Text("看直播", maxLines = 1)
                }
            }
        }
    }
}
