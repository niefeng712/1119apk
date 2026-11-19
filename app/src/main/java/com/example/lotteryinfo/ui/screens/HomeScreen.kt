package com.example.lotteryinfo.ui.screens

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.lotteryinfo.data.LotteryRepository
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.ui.components.LotteryBallRow
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.ui.theme.GoldAccent

@Composable
fun HomeScreen(
    sizeClass: ScreenSizeClass,
    onOpenHistory: (LotteryType) -> Unit,
    onOpenTrend: (LotteryType) -> Unit,
    onOpenLive: () -> Unit
) {
    var selectedOrg by remember { mutableStateOf(LotteryOrg.ALL) }
    var draws by remember { mutableStateOf<List<LotteryDraw>>(emptyList()) }
    var statusText by remember { mutableStateOf("正在同步福彩与体彩全部玩法最新开奖...") }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        val res = LotteryRepository.fetchLatestAll()
        draws = res.draws
        statusText = res.statusMessage
        isLoading = false
    }

    val filteredDraws = remember(selectedOrg, draws) {
        if (selectedOrg == LotteryOrg.ALL) draws
        else draws.filter { it.lotteryType.org == selectedOrg }
    }

    val columns = when (sizeClass) {
        ScreenSizeClass.COMPACT -> 1
        ScreenSizeClass.MEDIUM -> 2
        ScreenSizeClass.EXPANDED -> 2
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
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
                    onOpenHistory = { onOpenHistory(draw.lotteryType) },
                    onOpenTrend = { onOpenTrend(draw.lotteryType) },
                    onOpenLive = onOpenLive
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LatestDrawCard(
    draw: LotteryDraw,
    onOpenHistory: () -> Unit,
    onOpenTrend: () -> Unit,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${draw.lotteryType.displayName} · 第 ${draw.issue} 期",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${draw.lotteryType.org.label} | ${draw.drawDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "奖池 ${draw.poolAmount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            LotteryBallRow(
                redBalls = draw.redBalls,
                blueBalls = draw.blueBalls,
                padTwoDigits = twoDigits
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draw.lotteryType.drawSchedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onOpenHistory, label = { Text("历史开奖") })
                    AssistChip(onClick = onOpenTrend, label = { Text("走势图") })
                    AssistChip(onClick = onOpenLive, label = { Text("看直播") })
                }
            }
        }
    }
}
