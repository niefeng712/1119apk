package com.example.lotteryinfo.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lotteryinfo.data.LotteryRepository
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.ui.components.LotteryBallRow
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

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
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedType) {
        isLoading = true
        currentPage = 1
        val res = LotteryRepository.fetchHistoryByType(selectedType, pageNo = 1, pageSize = 100)
        history = res.draws
        selectedDraw = history.firstOrNull()
        canLoadMore = res.isFromNetwork && res.draws.size >= 50
        isLoading = false
    }

    val filtered = remember(history, query) {
        if (query.isBlank()) history
        else history.filter { it.issue.contains(query.trim()) || it.drawDate.contains(query.trim()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                label = { Text("可选筛选：输入期号或日期（已直接展示全部历史）") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "已加载 ${history.size} 期",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
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
                                    } else {
                                        canLoadMore = false
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("加载更多历史数据（再拉取 100 期）")
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

@Composable
private fun HistoryDrawCard(
    draw: LotteryDraw,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val twoDigits = !draw.lotteryType.isDigitGame
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "第 ${draw.issue} 期",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = draw.drawDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LotteryBallRow(
                redBalls = draw.redBalls,
                blueBalls = draw.blueBalls,
                padTwoDigits = twoDigits
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "本期销量：${draw.salesAmount}  |  一等奖：${draw.firstPrizeCount} 注（${draw.firstPrizeAmount}）",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F6368)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (draw == null) {
            Text(
                text = "请选择左侧期号查看详情",
                modifier = Modifier.padding(24.dp),
                color = Color.Gray
            )
        } else {
            val twoDigits = !draw.lotteryType.isDigitGame
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${draw.lotteryType.displayName} 第 ${draw.issue} 期详情",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "开奖日期：${draw.drawDate}（${draw.lotteryType.org.label}）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                LotteryBallRow(
                    redBalls = draw.redBalls,
                    blueBalls = draw.blueBalls,
                    padTwoDigits = twoDigits,
                    ballSize = 40.dp
                )
                Text(text = "奖池滚存：${draw.poolAmount}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "全国销量：${draw.salesAmount}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "一等奖：${draw.firstPrizeCount} 注，单注奖金 ${draw.firstPrizeAmount}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (draw.isLiveData) "数据来源：官方公开接口实时同步" else "数据来源：本地离线示例数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
