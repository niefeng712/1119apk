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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val maxNumber = if (showBlueZone && hasBlue) selectedType.blueMax else selectedType.redMax
    val startNumber = if (isDigit) 0 else 1
    val totalCols = maxNumber - startNumber + 1
    val ballColor = if (showBlueZone) BlueBallColor else RedBallColor
    val twoDigits = !isDigit

    val cellWidth = when {
        selectedType == LotteryType.KL8 -> 36.dp
        sizeClass == ScreenSizeClass.EXPANDED -> 44.dp
        else -> 38.dp
    }
    val cellHeight = if (sizeClass == ScreenSizeClass.EXPANDED) 42.dp else 38.dp
    val issueColWidth = if (sizeClass == ScreenSizeClass.COMPACT) 96.dp else 114.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
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

        val hScroll = rememberScrollState()
        val vScroll = rememberScrollState()

        // 表头固定在顶部，左侧期号与右侧单 Canvas 网格同步滚动，绝不截断、绝不错位
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Row(modifier = Modifier.background(Color(0xFFEDF0F5))) {
                Box(
                    modifier = Modifier.width(issueColWidth).height(cellHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "期号", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    for (num in startNumber..maxNumber) {
                        Box(
                            modifier = Modifier.width(cellWidth).height(cellHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (twoDigits) num.toString().padStart(2, '0') else num.toString(),
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxSize().verticalScroll(vScroll)) {
                // 左侧期号列
                Column(modifier = Modifier.width(issueColWidth)) {
                    draws.forEachIndexed { idx, draw ->
                        val bg = if (idx % 2 == 0) Color.White else Color(0xFFF8F9FB)
                        Box(
                            modifier = Modifier
                                .width(issueColWidth)
                                .height(cellHeight)
                                .background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = draw.issue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2329)
                            )
                        }
                    }
                }

                // 右侧整张大网格单 Canvas（零回收错位、支持 100 期流畅滑动）
                Box(modifier = Modifier.horizontalScroll(hScroll)) {
                    val gridWidth = cellWidth * totalCols
                    val gridHeight = cellHeight * draws.size

                    Canvas(modifier = Modifier.width(gridWidth).height(gridHeight)) {
                        val cw = cellWidth.toPx()
                        val ch = cellHeight.toPx()
                        val lineColor = Color(0xFFE0E3E7)

                        // 斑马纹背景
                        for (r in draws.indices) {
                            if (r % 2 == 1) {
                                drawRect(
                                    color = Color(0xFFF8F9FB),
                                    topLeft = Offset(0f, r * ch),
                                    size = androidx.compose.ui.geometry.Size(size.width, ch)
                                )
                            }
                        }

                        // 网格线
                        for (c in 0..totalCols) {
                            val x = c * cw
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        }
                        for (r in 0..draws.size) {
                            val y = r * ch
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        }

                        // 连线折线（蓝球或数字彩各位）
                        if (showBlueZone || isDigit) {
                            val points = draws.mapIndexedNotNull { rowIndex, draw ->
                                val target = if (showBlueZone) draw.blueBalls.firstOrNull()
                                else draw.redBalls.getOrNull(selectedDigitPos)
                                if (target == null) null
                                else Offset((target - startNumber + 0.5f) * cw, (rowIndex + 0.5f) * ch)
                            }
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = ballColor.copy(alpha = 0.65f),
                                    start = points[i],
                                    end = points[i + 1],
                                    strokeWidth = 3.5f
                                )
                            }
                        }

                        // 所有中奖球实心圆
                        draws.forEachIndexed { rowIndex, draw ->
                            val targets = when {
                                showBlueZone -> draw.blueBalls
                                isDigit -> listOfNotNull(draw.redBalls.getOrNull(selectedDigitPos))
                                else -> draw.redBalls
                            }
                            targets.forEach { num ->
                                val colIndex = num - startNumber
                                if (colIndex in 0 until totalCols) {
                                    drawCircle(
                                        color = ballColor,
                                        radius = minOf(cw, ch) * 0.43f,
                                        center = Offset((colIndex + 0.5f) * cw, (rowIndex + 0.5f) * ch)
                                    )
                                }
                            }
                        }
                    }

                    // 叠加白色加粗号码文字
                    Column {
                        draws.forEach { draw ->
                            val targets = when {
                                showBlueZone -> draw.blueBalls
                                isDigit -> listOfNotNull(draw.redBalls.getOrNull(selectedDigitPos))
                                else -> draw.redBalls
                            }.toSet()
                            Row {
                                for (num in startNumber..maxNumber) {
                                    Box(
                                        modifier = Modifier.width(cellWidth).height(cellHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (targets.contains(num)) {
                                            Text(
                                                text = if (twoDigits) num.toString().padStart(2, '0') else num.toString(),
                                                color = Color.White,
                                                fontSize = 13.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
