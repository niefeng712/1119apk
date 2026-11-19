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
    var draws by remember { mutableStateOf<List<LotteryDraw>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedType) {
        isLoading = true
        draws = LotteryRepository.fetchHistoryByType(selectedType, count = 25).draws
        isLoading = false
    }

    val hasBlue = selectedType.blueMax > 0
    if (!hasBlue && showBlueZone) {
        showBlueZone = false
    }

    val maxNumber = if (showBlueZone && hasBlue) selectedType.blueMax else selectedType.redMax
    val startNumber = if (selectedType == LotteryType.FC3D || selectedType == LotteryType.PL3) 0 else 1
    val totalCols = maxNumber - startNumber + 1
    val ballColor = if (showBlueZone) BlueBallColor else RedBallColor

    // 9寸–14寸平板采用更宽敞的单元格，提升大屏可读性
    val cellWidth = if (sizeClass == ScreenSizeClass.EXPANDED) 38.dp else 32.dp
    val cellHeight = if (sizeClass == ScreenSizeClass.EXPANDED) 38.dp else 34.dp
    val issueColWidth = if (sizeClass == ScreenSizeClass.COMPACT) 80.dp else 96.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            if (hasBlue && sizeClass != ScreenSizeClass.COMPACT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        if (hasBlue && sizeClass == ScreenSizeClass.COMPACT) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        Spacer(modifier = Modifier.height(8.dp))

        val hScroll = rememberScrollState()
        val vScroll = rememberScrollState()
        val twoDigits = selectedType != LotteryType.FC3D && selectedType != LotteryType.PL3

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(vScroll)
        ) {
            Row(modifier = Modifier.background(Color(0xFFEDF0F5))) {
                Text(
                    text = "期号",
                    modifier = Modifier
                        .width(issueColWidth)
                        .height(cellHeight)
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    for (num in startNumber..maxNumber) {
                        Text(
                            text = if (twoDigits) num.toString().padStart(2, '0') else num.toString(),
                            modifier = Modifier
                                .width(cellWidth)
                                .height(cellHeight)
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row {
                Column(modifier = Modifier.width(issueColWidth)) {
                    draws.forEach { draw ->
                        Box(
                            modifier = Modifier
                                .width(issueColWidth)
                                .height(cellHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = draw.issue,
                                fontSize = 12.sp,
                                color = Color(0xFF444746)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.horizontalScroll(hScroll)) {
                    val gridWidth = cellWidth * totalCols
                    val gridHeight = cellHeight * draws.size

                    Canvas(
                        modifier = Modifier
                            .width(gridWidth)
                            .height(gridHeight)
                    ) {
                        val cw = cellWidth.toPx()
                        val ch = cellHeight.toPx()
                        val lineColor = Color(0xFFE0E3E7)

                        for (c in 0..totalCols) {
                            val x = c * cw
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        }
                        for (r in 0..draws.size) {
                            val y = r * ch
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        }

                        if (showBlueZone) {
                            val points = draws.mapIndexedNotNull { rowIndex, draw ->
                                val target = draw.blueBalls.firstOrNull() ?: return@mapIndexedNotNull null
                                val colIndex = target - startNumber
                                Offset((colIndex + 0.5f) * cw, (rowIndex + 0.5f) * ch)
                            }
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = ballColor.copy(alpha = 0.6f),
                                    start = points[i],
                                    end = points[i + 1],
                                    strokeWidth = 3f
                                )
                            }
                        }

                        draws.forEachIndexed { rowIndex, draw ->
                            val targets = if (showBlueZone) draw.blueBalls else draw.redBalls
                            targets.forEach { num ->
                                val colIndex = num - startNumber
                                if (colIndex in 0 until totalCols) {
                                    drawCircle(
                                        color = ballColor,
                                        radius = (minOf(cw, ch) * 0.36f),
                                        center = Offset((colIndex + 0.5f) * cw, (rowIndex + 0.5f) * ch)
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        draws.forEach { draw ->
                            val targets = (if (showBlueZone) draw.blueBalls else draw.redBalls).toSet()
                            Row {
                                for (num in startNumber..maxNumber) {
                                    val hit = targets.contains(num)
                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .height(cellHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hit) {
                                            Text(
                                                text = if (twoDigits) num.toString().padStart(2, '0') else num.toString(),
                                                color = Color.White,
                                                fontSize = 11.sp,
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
