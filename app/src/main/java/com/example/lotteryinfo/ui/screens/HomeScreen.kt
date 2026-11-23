package com.example.lotteryinfo.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lotteryinfo.data.LotteryRepository
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.model.SINA_SOURCE_MARK
import com.example.lotteryinfo.model.displayPool
import com.example.lotteryinfo.model.isSample
import com.example.lotteryinfo.model.isSinaFast
import com.example.lotteryinfo.ui.components.LotteryBallRow
import com.example.lotteryinfo.ui.components.StatusPill
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.ui.theme.FcBrandColor
import com.example.lotteryinfo.ui.theme.GoldAccent
import com.example.lotteryinfo.ui.theme.MutedGray
import com.example.lotteryinfo.ui.theme.SubtleText
import com.example.lotteryinfo.ui.theme.SuccessGreen
import com.example.lotteryinfo.ui.theme.TcBrandColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val SINA_OPEN_URL = "https://lotto.sina.cn/open/"
private val BEIJING_TZ: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

private fun beijingNow(pattern: String): String =
    SimpleDateFormat(pattern, Locale.CHINA).apply { timeZone = BEIJING_TZ }.format(Date())

private fun beijingMinutes(): Int {
    val cal = Calendar.getInstance(BEIJING_TZ)
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

/** 21:14–21:40 为开奖高峰窗口，新浪秒级同步 */
private fun inDrawWindow(): Boolean = beijingMinutes() in (21 * 60 + 14)..(21 * 60 + 40)

/** 轮询间隔：开奖窗口 8 秒；21 点后 1 分钟；白天 5 分钟（白天不会出新号，省电省流量） */
private fun pollIntervalMs(): Long {
    val m = beijingMinutes()
    return when {
        m in (21 * 60 + 14)..(21 * 60 + 40) -> 8_000L
        m >= 21 * 60 -> 60_000L
        else -> 300_000L
    }
}

private fun allDoneToday(): Boolean = LotteryType.entries.all { LotteryRepository.isDoneForToday(it) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    sizeClass: ScreenSizeClass,
    onOpenHistory: (LotteryType) -> Unit,
    onOpenLive: (LotteryOrg) -> Unit
) {
    var selectedOrg by remember { mutableStateOf(LotteryOrg.ALL) }
    var draws by remember { mutableStateOf(LotteryRepository.getAllLatestSnapshot()) }
    var statusText by remember { mutableStateOf("正在同步最新开奖...") }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var sinaSyncTrigger by remember { mutableIntStateOf(1) }
    var lastSyncTime by remember { mutableStateOf("--:--:--") }

    // 拉取官方接口（福彩网 / 体彩网），结果写入缓存后刷新界面
    suspend fun syncOfficial() {
        val res = LotteryRepository.fetchLatestAll()
        draws = LotteryRepository.getAllLatestSnapshot()
        lastSyncTime = beijingNow("HH:mm:ss")
        statusText = when {
            allDoneToday() -> "今日开奖已全部更新"
            res.isFromNetwork -> "已同步官方数据"
            else -> "官方接口暂不可用，等待新浪快讯"
        }
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) sinaSyncTrigger++
        isLoading = true
        syncOfficial()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            delay(pollIntervalMs())
            if (allDoneToday()) {
                draws = LotteryRepository.getAllLatestSnapshot()
                statusText = "今日开奖已全部更新"
                break
            }
            tick++
            val window = inDrawWindow()
            if (window) sinaSyncTrigger++
            // 开奖窗口内新浪每 8 秒刷新，官方接口每 4 轮（约 32 秒）补一次奖级明细
            if (!window || tick % 4 == 0) {
                syncOfficial()
            } else {
                draws = LotteryRepository.getAllLatestSnapshot()
            }
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
                    SummaryHeader(
                        draws = draws,
                        statusText = statusText,
                        lastSyncTime = lastSyncTime,
                        isLoading = isLoading,
                        onRefresh = { refreshTrigger++ }
                    )
                }
                item(span = { GridItemSpan(columns) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LotteryOrg.entries.forEach { org ->
                            FilterChip(
                                selected = selectedOrg == org,
                                onClick = { selectedOrg = org },
                                label = { Text(org.label) }
                            )
                        }
                    }
                }
                items(filteredDraws, key = { it.lotteryType.id }) { draw ->
                    LatestDrawCard(
                        draw = draw,
                        syncTime = LotteryRepository.getSyncTime(draw.lotteryType).ifBlank { lastSyncTime },
                        onOpenHistory = { onOpenHistory(draw.lotteryType) },
                        onOpenLive = { onOpenLive(draw.lotteryType.org) }
                    )
                }
            }
        }

        SinaSyncWebView(trigger = sinaSyncTrigger) { nowTime ->
            draws = LotteryRepository.getAllLatestSnapshot()
            lastSyncTime = nowTime
            statusText = if (allDoneToday()) "今日开奖已全部更新" else "新浪快讯同步成功"
        }
    }
}

/** 顶部渐变概览卡：日期 + 今日开奖票种进度 + 同步状态 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryHeader(
    draws: List<LotteryDraw>,
    statusText: String,
    lastSyncTime: String,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    // draws 作为参数传入，数据变化时重新计算各票种状态
    val todayTypes = remember(draws) { LotteryType.entries.filter { LotteryRepository.isDrawDayToday(it) } }
    val doneSet = remember(draws) { todayTypes.filter { LotteryRepository.isUpdatedForToday(it) }.toSet() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFB71C1C), Color(0xFFE53935), Color(0xFFF57C00))
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = beijingNow("M月d日 EEEE"),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "今日开奖 ${todayTypes.size} 个票种 · 已出 ${doneSet.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                TextButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                todayTypes.forEach { type ->
                    val done = type in doneSet
                    Text(
                        text = (if (done) "✓ " else "○ ") + type.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = if (done) 0.30f else 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                text = "$statusText · 更新于 $lastSyncTime",
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LatestDrawCard(
    draw: LotteryDraw,
    syncTime: String,
    onOpenHistory: () -> Unit,
    onOpenLive: () -> Unit
) {
    val type = draw.lotteryType
    val brand = if (type.org == LotteryOrg.FC) FcBrandColor else TcBrandColor
    val drawToday = LotteryRepository.isDrawDayToday(type)
    val updatedToday = LotteryRepository.isUpdatedForToday(type)
    val (badgeText, badgeColor) = when {
        draw.isSample -> "示例数据" to MutedGray
        drawToday && updatedToday -> "今日已开奖" to SuccessGreen
        drawToday -> "今晚开奖" to GoldAccent
        else -> "今日不开奖" to MutedGray
    }
    val sourceText = when {
        draw.isSample -> "离线示例"
        draw.isSinaFast -> "新浪快讯（奖级稍后补齐）"
        else -> "官方开奖"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // 顶部品牌色条：福彩红 / 体彩蓝
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(brand, brand.copy(alpha = 0.35f))))
            )
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(text = badgeText, color = badgeColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "第 ${draw.issue} 期 · ${draw.drawDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtleText
                )

                Spacer(modifier = Modifier.height(14.dp))
                LotteryBallRow(
                    redBalls = draw.redBalls,
                    blueBalls = draw.blueBalls,
                    padTwoDigits = !type.isDigitGame
                )

                draw.displayPool?.let { pool ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "奖池滚存 $pool",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${type.org.label} · ${type.drawSchedule}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtleText
                )
                Text(
                    text = "$sourceText · 更新于 $syncTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray
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
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("历史开奖", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenLive,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = brand),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("看直播", maxLines = 1)
                    }
                }
            }
        }
    }
}

/** 隐藏的新浪开奖页 WebView：抓取页面上已出的号码，作为官方接口之前的“快讯” */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SinaSyncWebView(trigger: Int, onUpdated: (String) -> Unit) {
    val latestOnUpdated by rememberUpdatedState(onUpdated)
    AndroidView(
        modifier = Modifier
            .size(1.dp)
            .alpha(0f),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val wv = view ?: return
                        // 页面为前端渲染，分两次抓取，避免第一次时号码还没渲染出来
                        listOf(1_500L, 4_500L).forEach { delayMs ->
                            wv.postDelayed({
                                wv.evaluateJavascript(SINA_EXTRACT_JS) { res ->
                                    val now = beijingNow("HH:mm:ss")
                                    if (applySinaResult(res, now)) latestOnUpdated(now)
                                }
                            }, delayMs)
                        }
                    }
                }
                loadUrl(SINA_OPEN_URL)
            }
        },
        update = { wv ->
            if (wv.tag != trigger) {
                val firstBind = wv.tag == null
                wv.tag = trigger
                // 首次绑定时页面刚开始加载，无需立刻 reload
                if (!firstBind && !allDoneToday()) wv.reload()
            }
        }
    )
}

/** 解析 JS 返回的 "KEY|期号|MM-dd|n1,n2,..;..."，写入缓存。返回是否有更新 */
private fun applySinaResult(res: String?, nowTime: String): Boolean {
    val cleaned = res?.trim()?.removeSurrounding("\"") ?: return false
    if (cleaned.isEmpty() || cleaned == "null") return false
    val cal = Calendar.getInstance(BEIJING_TZ)
    val curYear = cal.get(Calendar.YEAR)
    val curMonth = cal.get(Calendar.MONTH) + 1
    var updatedAny = false
    cleaned.split(";").forEach { item ->
        val parts = item.split("|")
        if (parts.size != 4) return@forEach
        val type = when (parts[0]) {
            "FC3D" -> LotteryType.FC3D
            "PL3" -> LotteryType.PL3
            "PL5" -> LotteryType.PL5
            "DLT" -> LotteryType.DLT
            else -> null
        } ?: return@forEach
        val issue = parts[1].trim()
        val datePart = parts[2].trim()
        val isDlt = type == LotteryType.DLT
        val nums = parts[3].split(",").mapNotNull {
            val s = it.trim()
            if (isDlt) {
                s.takeIf { v -> v.length == 2 && v.all { c -> c.isDigit() } }?.toIntOrNull()
            } else {
                s.takeIf { v -> v.length == 1 && v[0].isDigit() }?.toIntOrNull()
            }
        }
        val expectedCount = when (type) {
            LotteryType.PL5 -> 5
            LotteryType.DLT -> type.redCount + type.blueCount
            else -> 3
        }
        // 大乐透号码校验：前区 1–35 且不重复，后区 1–12 且不重复
        val rangeOk = !isDlt || (
            nums.size == expectedCount &&
                nums.take(type.redCount).all { n -> n in 1..type.redMax } &&
                nums.drop(type.redCount).all { n -> n in 1..type.blueMax } &&
                nums.take(type.redCount).toSet().size == type.redCount &&
                nums.drop(type.redCount).toSet().size == type.blueCount
            )
        if (issue.isEmpty() || nums.size != expectedCount || !rangeOk) return@forEach
        // 跨年处理：1 月看到上一年 12 月的开奖
        val month = datePart.substringBefore("-").toIntOrNull() ?: 0
        val year = if (month > curMonth) curYear - 1 else curYear
        val drawDate = if (datePart.isNotEmpty()) "$year-$datePart" else ""
        val reds = if (isDlt) nums.take(type.redCount) else nums
        val blues = if (isDlt) nums.drop(type.redCount) else emptyList()
        val draw = LotteryDraw(
            lotteryType = type,
            issue = issue,
            drawDate = drawDate,
            redBalls = reds,
            blueBalls = blues,
            poolAmount = SINA_SOURCE_MARK,
            salesAmount = "",
            firstPrizeCount = 0,
            firstPrizeAmount = "",
            isLiveData = true
        )
        LotteryRepository.updateFastLatest(draw, nowTime)
        updatedAny = true
    }
    return updatedAny
}

private val SINA_EXTRACT_JS = """
(function() {
    var others = ['双色球', '快乐8', '七星彩', '七乐彩'];
    var targets = [
        { key: 'FC3D', names: ['福彩3D'], count: 3, exclude: ['大乐透', '排列3', '排列三', '排列5', '排列五'] },
        { key: 'PL3', names: ['排列3', '排列三'], count: 3, exclude: ['大乐透', '福彩3D', '排列5', '排列五'] },
        { key: 'PL5', names: ['排列5', '排列五'], count: 5, exclude: ['大乐透', '福彩3D', '排列3', '排列三'] },
        { key: 'DLT', names: ['大乐透'], count: 7, twoDigit: true, exclude: ['福彩3D', '排列3', '排列三', '排列5', '排列五'] }
    ];
    var allEls = Array.prototype.slice.call(document.querySelectorAll('li, tr, div, section'));
    var out = [];
    targets.forEach(function(t) {
        var numRe = t.twoDigit ? /^[0-9]{2}$/ : /^[0-9]$/;
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
                var children = Array.prototype.slice.call(nodes[ni].children || []);
                if (children.length !== t.count) continue;
                var ok = true;
                var nums = [];
                for (var ki = 0; ki < children.length; ki++) {
                    var ch = children[ki];
                    if (ch.children && ch.children.length > 0) { ok = false; break; }
                    var val = (ch.textContent || '').trim();
                    if (!numRe.test(val)) { ok = false; break; }
                    nums.push(val);
                }
                if (ok && nums.length === t.count) { balls = nums; break; }
            }
            // 乐透型：前区、后区号码常放在两个不同的容器里，按文档顺序收集叶子节点
            if (!balls && t.twoDigit) {
                var leaves = Array.prototype.slice.call(card.querySelectorAll('*')).filter(function(el) {
                    return (!el.children || el.children.length === 0) && numRe.test((el.textContent || '').trim());
                });
                if (leaves.length >= t.count) {
                    balls = leaves.slice(0, t.count).map(function(el) { return el.textContent.trim(); });
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
