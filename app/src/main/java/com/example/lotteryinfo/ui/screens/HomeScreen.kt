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
        val snapshot = LotteryRepository.getAllLatestSnapshot()
        if (snapshot.isNotEmpty()) {
            draws = snapshot
            val nowTime = formatBeijingTime()
            lastSyncTime = nowTime
            statusText = if (LotteryType.entries.all { LotteryRepository.isUpdatedForToday(it) }) {
                "今日开奖已全部更新（新浪同步 $nowTime）"
            } else {
                "新浪同步中..."
            }
        } else {
            val res = LotteryRepository.fetchLatestAll()
            draws = res.draws
            statusText = res.statusMessage
        }
        isLoading = false
lastSyncTime = formatBeijingTime()
    }
    LaunchedEffect(Unit) {
        while (true) {
            if (LotteryType.entries.all { LotteryRepository.isUpdatedForToday(it) }) {
                val nowTime = formatBeijingTime()
                lastSyncTime = nowTime
                statusText = "今日开奖已全部更新（新浪同步 $nowTime）"
                break
            }
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
            val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val intervalMs = if (minutes in (21 * 60 + 14)..(21 * 60 + 40)) 8_000L else 60_000L    
            delay(intervalMs)
            if (minutes in (21 * 60 + 14)..(21 * 60 + 40)) sinaSyncTrigger++
            if (LotteryType.entries.all { LotteryRepository.isUpdatedForToday(it) }) {
                val nowTime = formatBeijingTime()
                lastSyncTime = nowTime
                statusText = "今日开奖已全部更新（新浪同步 $nowTime）"
                break
            }
            val snapshot = LotteryRepository.getAllLatestSnapshot()
            if (snapshot.isNotEmpty()) {
                draws = snapshot
            } else {
                val res = LotteryRepository.fetchLatestAll()
                draws = res.draws
                statusText = res.statusMessage
            }
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
        AndroidView(
            modifier = Modifier.size(1.dp).alpha(0f),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.postDelayed({
                                val js = """
                                    (function() {
                                        var others = ['双色球', '大乐透', '快乐8', '七星彩', '七乐彩'];
                                        var targets = [
                                            { key: 'FC3D', names: ['福彩3D'], count: 3, exclude: ['排列3', '排列三', '排列5', '排列五'] },
                                            { key: 'PL3', names: ['排列3', '排列三'], count: 3, exclude: ['福彩3D', '排列5', '排列五'] },
                                            { key: 'PL5', names: ['排列5', '排列五'], count: 5, exclude: ['福彩3D', '排列3', '排列三'] }
                                        ];
                                        var allEls = Array.prototype.slice.call(document.querySelectorAll('li, tr, div, section'));
                                        var out = [];
                                        targets.forEach(function(t) {
                                            var candidates = allEls.filter(function(el) {
                                                var txt = (el.innerText || '').trim();
                                                if (!txt) return false;
                                                var hasName = t.names.some(function(n) { return txt.indexOf(n) !== -1; });
                                                if (!hasName) return false;
                                                var hasOther = others.concat(t.exclude).some(function(o) { return txt.indexOf(o) !== -1; });
                                                return !hasOther;
                                            });
                                            if (!candidates.length) return;
                                            candidates.sort(function(a, b) {
                                                return (a.innerText || '').length - (b.innerText || '').length;
                                            });
                                            for (var ci = 0; ci < candidates.length; ci++) {
                                                var card = candidates[ci];
                                                var cardText = card.innerText || '';
                                                var issueMatch = cardText.match(/(202\d{4}|2\d{4})\s*期/);
                                                if (!issueMatch) continue;
                                                var dateMatch = cardText.match(/(\d{2}-\d{2})/);
                                                var balls = null;
                                                var nodes = [card].concat(Array.prototype.slice.call(card.querySelectorAll('*')));
                                                for (var ni = 0; ni < nodes.length; ni++) {
                                                    var parent = nodes[ni];
                                                    var children = Array.prototype.slice.call(parent.children || []);
                                                    if (children.length !== t.count) continue;
                                                    var ok = true;
                                                    var nums = [];
                                                    for (var ki = 0; ki < children.length; ki++) {
                                                        var ch = children[ki];
                                                        if (ch.children && ch.children.length > 0) { ok = false; break; }
                                                        var val = (ch.textContent || '').trim();
                                                        if (!/^[0-9]$/.test(val)) { ok = false; break; }
                                                        nums.push(val);
                                                    }
                                                    if (ok && nums.length === t.count) {
                                                        balls = nums;
                                                        break;
                                                    }
                                                }
                                                if (balls) {
                                                    out.push(t.key + '|' + issueMatch[1] + '|' + (dateMatch ? dateMatch[1] : '') + '|' + balls.join(','));
                                                    break;
                                                }
                                            }
                                        });
                                        return out.join(';');
                                    })()
                                """.trimIndent()
                                view.evaluateJavascript(js) { res ->
                                    val cleaned = res?.trim()?.removeSurrounding("\"") ?: ""
                                    if (cleaned.isNotEmpty() && cleaned != "null") {
                                        val nowTime = formatBeijingTime()
                                        val year = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).get(Calendar.YEAR)
                                        var updatedAny = false
                                        cleaned.split(";").forEach { item ->
                                            val parts = item.split("|")
                                            if (parts.size == 4) {
                                                val type = when (parts[0]) {
                                                    "FC3D" -> LotteryType.FC3D
                                                    "PL3" -> LotteryType.PL3
                                                    "PL5" -> LotteryType.PL5
                                                    else -> null
                                                }
                                                val issue = parts[1].trim()
                                                val datePart = parts[2].trim()
                                                val nums = parts[3].split(",").mapNotNull {
                                                    it.trim().takeIf { s -> s.length == 1 && s[0].isDigit() }?.toIntOrNull()
                                                }
                                                val expectedCount = if (type == LotteryType.PL5) 5 else 3
                                                if (type != null && issue.isNotEmpty() && nums.size == expectedCount) {
                                                    val drawDate = if (datePart.isNotEmpty()) "$year-$datePart" else ""
                                                    val draw = LotteryDraw(type, issue, drawDate, nums, emptyList(), "新浪", "", 0, "")
                                                    LotteryRepository.updateFastLatest(draw, nowTime)
                                                    updatedAny = true
                                                }
                                            }
                                        }
                                        if (updatedAny) {
                                            draws = LotteryRepository.getAllLatestSnapshot()
                                            lastSyncTime = nowTime
                                            statusText = if (LotteryType.entries.all { LotteryRepository.isUpdatedForToday(it) }) {
                                                "今日开奖已全部更新（新浪同步 $nowTime）"
                                            } else {
                                                "新浪同步成功"
                                            }
                                        }
                                    }
                                }
                            }, 1500L)
                        }
                    }
                    loadUrl("https://lotto.sina.cn/open/")
                }
            },
            update = { wv ->
                if (wv.tag != sinaSyncTrigger) {
                    wv.tag = sinaSyncTrigger
                    if (!LotteryType.entries.all { LotteryRepository.isUpdatedForToday(it) }) {
                        wv.reload()
                    }
                }
            }
        )
    }
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
