package com.example.lotteryinfo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryinfo.data.LotteryRepository
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.ui.theme.BlueBallColor
import com.example.lotteryinfo.ui.theme.RedBallColor

@Composable
fun TrendChartScreen(
    sizeClass: ScreenSizeClass,
    initialType: LotteryType
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var showBlueZone by remember { mutableStateOf(false) }
    var selectedDigitPos by remember { mutableIntStateOf(0) }
    var draws by remember { mutableStateOf<List<LotteryDraw>>(emptyList()) }
    var statusText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedType) {
        isLoading = true
        showBlueZone = false
        selectedDigitPos = 0
        val res = LotteryRepository.fetchHistoryByType(selectedType, pageNo = 1, pageSize = 100)
        draws = res.draws
        statusText = res.statusMessage
        isLoading = false
    }

    val hasBlue = selectedType.blueMax > 0
    val isDigit = selectedType.isDigitGame
    val maxNumber = when {
        showBlueZone && hasBlue -> selectedType.blueMax
        else -> selectedType.redMax
    }
    val startNumber = if (isDigit) 0 else 1
    val totalCols = maxNumber - startNumber + 1
    val ballColor = if (showBlueZone) BlueBallColor else RedBallColor
    val twoDigits = !isDigit

    val cellWidth = when {
        selectedType == LotteryType.KL8 -> 34.dp
        sizeClass == ScreenSizeClass.EXPANDED -> 44.dp
        else -> 38.dp
    }
    val cellHeight = if (sizeClass == ScreenSizeClass.EXPANDED) 42.dp else 38.dp
    val issueColWidth = if (sizeClass == ScreenSizeClass.COMPACT) 96.dp else 112.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 彩种选择栏
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            LotteryType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.displayName) }
                )
            }
        }

        // 分区 / 位数切换与数据量提示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                if (isDigit) {
                    val posCount = if (selectedType == LotteryType.QXC) 7 else selectedType.digitCount
                    for (pos in 0 until posCount) {
                        val label = if (selectedType == LotteryType.QXC && pos == 6) "第7位(蓝)"
                        else "第${pos + 1}位"
                        FilterChip(
                            selected = (!showBlueZone && selectedDigitPos == pos) || (showBlueZone && pos == 6),
                            onClick = {
                                if (selectedType == LotteryType.QXC && pos == 6) {
                                    showBlueZone = true
                                } else {
                                    showBlueZone = false
                                    selectedDigitPos = pos
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                } else if (hasBlue) {
                    FilterChip(
                        selected = !showBlueZone,
                        onClick = { showBlueZone = false },
                        label = { Text("红球走势") }
                    )
                    FilterChip(
                        selected = showBlueZone,
                        onClick = { showBlueZone = true },
                        label = { Text("蓝球走势") }
                    )
                }
            }

            if (statusText.isNotBlank()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 统一横向滚动容器：保证表头与所有期号行完美对齐，不截断、不漂移
        val hScroll = rememberScrollState()

        Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // 左侧固定「期号」列 + 右侧「号码网格」同步纵向 LazyColumn
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // 固定表头行
                item {
                    Row(modifier = Modifier.background(Color(0xFFEDF0F5))) {
                        Text(
                            text = "期号",
                            modifier = Modifier
                                .width(issueColWidth)
                                .height(cellHeight)
                                .padding(top = 7.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(modifier = Modifier.horizontalScroll(hScroll)) {
                            for (num in startNumber..maxNumber) {
                                Text(
                                    text = if (twoDigits) num.toString().padStart(2, '0') else num.toString(),
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .height(cellHeight)
                                        .padding(top = 7.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 每一期数据行
                itemsIndexed(draws) { index, draw ->
                    val rowBg = if (index % 2 == 0) Color.White else Color(0xFFF8F9FB)
                    val targets: List<Int> = when {
                        showBlueZone -> draw.blueBalls
                        isDigit -> listOfNotNull(draw.redBalls.getOrNull(selectedDigitPos))
                        else -> draw.redBalls
                    }
                    val prevTargets: List<Int> = if (index > 0) {
                        val prev = draws[index - 1]
                        when {
                            showBlueZone -> prev.blueBalls
                            isDigit -> listOfNotNull(prev.redBalls.getOrNull(selectedDigitPos))
                            else -> emptyList()
                        }
                    } else emptyList()

                    Row(modifier = Modifier.background(rowBg)) {
                        Box(
                            modifier = Modifier
                                .width(issueColWidth)
                                .height(cellHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = draw.issue,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2329),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(modifier = Modifier.horizontalScroll(hScroll)) {
                            val gridWidth = cellWidth * totalCols
                            Canvas(
                                modifier = Modifier
                                    .width(gridWidth)
                                    .height(cellHeight)
                            ) {
                                val cw = cellWidth.toPx()
                                val ch = cellHeight.toPx()
                                val lineColor = Color(0xFFE6E9EE)

                                // 网格线
                                for (c in 0..totalCols) {
                                    val x = c * cw
                                    drawLine(lineColor, Offset(x, 0f), Offset(x, ch), strokeWidth = 1f)
                                }
                                drawLine(lineColor, Offset(0f, ch), Offset(size.width, ch), strokeWidth = 1f)

                                // 与上一期连接趋势折线（单球/蓝球/数字彩位次）
                                val currVal = targets.firstOrNull()
                                val prevVal = prevTargets.firstOrNull()
                                if ((showBlueZone || isDigit) && currVal != null && prevVal != null) {
                                    val x1 = (prevVal - startNumber + 0.5f) * cw
                                    val x2 = (currVal - startNumber + 0.5f) * cw
                                    drawLine(
                                        color = ballColor.copy(alpha = 0.55f),
                                        start = Offset(x1, -ch * 0.5f),
                                        end = Offset(x2, ch * 0.5f),
                                        strokeWidth = 3f
                                    )
                                }

                                // 绘制中奖球圆点
                                targets.forEach { num ->
                                    val colIndex = num - startNumber
                                    if (colIndex in 0 until totalCols) {
                                        drawCircle(
                                            color = ballColor,
                                            radius = minOf(cw, ch) * 0.36f,
                                            center = Offset((colIndex + 0.5f) * cw, ch * 0.5f)
                                        )
                                    }
                                }
                            }

                            Row {
                                val targetSet = targets.toSet()
                                for (num in startNumber..maxNumber) {
                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .height(cellHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (targetSet.contains(num)) {
                                            Text(
                                                text = if (twoDigits) num.toString().padStart(2, '0') else num.toString(),
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
