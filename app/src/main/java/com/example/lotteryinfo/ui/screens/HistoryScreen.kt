package com.example.lotteryinfo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lotteryinfo.data.LotteryRepository
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.model.displayPool
import com.example.lotteryinfo.model.hasPrizeDetail
import com.example.lotteryinfo.model.isSample
import com.example.lotteryinfo.model.isSinaFast
import com.example.lotteryinfo.model.orDash
import com.example.lotteryinfo.ui.components.LotteryBallRow
import com.example.lotteryinfo.ui.components.StatusPill
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.ui.theme.FcBrandColor
import com.example.lotteryinfo.ui.theme.GoldAccent
import com.example.lotteryinfo.ui.theme.MutedGray
import com.example.lotteryinfo.ui.theme.SubtleText
import com.example.lotteryinfo.ui.theme.TcBrandColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    sizeClass: ScreenSizeClass,
    initialType: LotteryType
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var query by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<LotteryDraw>>(emptyList()) }
    var selectedDraw by remember { mutableStateOf<LotteryDraw?>(null) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var canLoadMore by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedType) {
        isLoading = true
        currentPage = 1
        query = ""
        val res = LotteryRepository.fetchHistoryByType(selectedType, pageNo = 1, pageSize = 100)
        history = res.draws
        selectedDraw = history.firstOrNull()
        canLoadMore = res.isFromNetwork && res.draws.size >= 50
        statusText = res.statusMessage
        isLoading = false
    }

    val filtered = remember(history, query) {
        val q = query.trim()
        if (q.isBlank()) history
        else history.filter { it.issue.contains(q) || it.drawDate.contains(q) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LotteryType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.displayName) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("筛选期号或日期") },
                placeholder = { Text("如 26133 或 11-22") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "共 ${history.size} 期",
                    style = MaterialTheme.typography.labelMedium,
                    color = SubtleText
                )
                Text(
                    text = selectedType.drawSchedule,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray
                )
            }
        }
        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTwoPane = maxWidth >= 840.dp && maxWidth > maxHeight
            val listContent: @Composable (Modifier) -> Unit = { mod ->
                LazyColumn(
                    modifier = mod,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isLoading && filtered.isEmpty()) {
                        item {
                            Text(
                                text = if (query.isBlank()) "暂无开奖数据" else "没有找到「${query.trim()}」相关期号",
                                color = MutedGray,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    items(filtered) { draw ->
                        HistoryDrawCard(
                            draw = draw,
                            isSelected = isTwoPane && draw.issue == selectedDraw?.issue,
                            onClick = { selectedDraw = draw }
                        )
                    }
                    if (canLoadMore && query.isBlank()) {
                        item {
                            Button(
                                enabled = !isLoading,
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val nextPage = currentPage + 1
                                        val more = LotteryRepository.fetchHistoryByType(
                                            selectedType,
                                            pageNo = nextPage,
                                            pageSize = 100
                                        )
                                        if (more.draws.isNotEmpty()) {
                                            history = (history + more.draws).distinctBy { it.issue }
                                            currentPage = nextPage
                                            canLoadMore = more.draws.size >= 50
                                        } else {
                                            canLoadMore = false
                                        }
                                        isLoading = false
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isLoading) "加载中..." else "加载更多（再拉取 100 期）")
                            }
                        }
                    }
                }
            }

            if (isTwoPane) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listContent(Modifier.weight(0.55f).fillMaxHeight())
                    DetailPanel(
                        draw = selectedDraw ?: filtered.firstOrNull(),
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    )
                }
            } else {
                listContent(Modifier.fillMaxSize())
            }
        }
    }
}

private fun prizeSummary(draw: LotteryDraw): String = when {
    draw.isSample -> "离线示例数据，联网后自动更新"
    draw.isSinaFast -> "新浪快讯：号码已出，奖级明细稍后同步官方"
    draw.hasPrizeDetail -> "销量 ${draw.salesAmount.orDash()}  ·  一等奖 ${draw.firstPrizeCount} 注（${draw.firstPrizeAmount.orDash()}）"
    else -> "奖级明细暂无"
}

@Composable
private fun HistoryDrawCard(
    draw: LotteryDraw,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val brand = if (draw.lotteryType.org == LotteryOrg.FC) FcBrandColor else TcBrandColor
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .background(brand, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "第 ${draw.issue} 期",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = draw.drawDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = SubtleText
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LotteryBallRow(
                    redBalls = draw.redBalls,
                    blueBalls = draw.blueBalls,
                    padTwoDigits = !draw.lotteryType.isDigitGame
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = prizeSummary(draw),
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtleText
                )
        }
    }
}

@Composable
private fun DetailPanel(
    draw: LotteryDraw?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (draw == null) {
            Text(
                text = "请选择左侧期号查看详情",
                modifier = Modifier.padding(24.dp),
                color = MutedGray
            )
        } else {
            val (sourceText, sourceColor) = when {
                draw.isSample -> "离线示例" to MutedGray
                draw.isSinaFast -> "新浪快讯" to GoldAccent
                else -> "官方数据" to (if (draw.lotteryType.org == LotteryOrg.FC) FcBrandColor else TcBrandColor)
            }
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${draw.lotteryType.displayName} 第 ${draw.issue} 期",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(text = sourceText, color = sourceColor)
                }
                Text(
                    text = "开奖日期：${draw.drawDate}（${draw.lotteryType.org.label}）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtleText
                )
                LotteryBallRow(
                    redBalls = draw.redBalls,
                    blueBalls = draw.blueBalls,
                    padTwoDigits = !draw.lotteryType.isDigitGame,
                    ballSize = 44.dp
                )
                HorizontalDivider()
                DetailLine("奖池滚存", draw.displayPool ?: "--")
                DetailLine("全国销量", draw.salesAmount.orDash())
                DetailLine(
                    "一等奖",
                    if (draw.hasPrizeDetail) "${draw.firstPrizeCount} 注 · 单注 ${draw.firstPrizeAmount.orDash()}" else "--"
                )
                if (draw.isSinaFast) {
                    Text(
                        text = "号码来自新浪快讯，官方公布后自动补齐奖级明细",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = SubtleText,
            modifier = Modifier.width(96.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
