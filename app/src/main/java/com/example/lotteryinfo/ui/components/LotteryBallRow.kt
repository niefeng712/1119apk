package com.example.lotteryinfo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryinfo.ui.theme.BlueBallColor
import com.example.lotteryinfo.ui.theme.MutedGray
import com.example.lotteryinfo.ui.theme.RedBallColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LotteryBallRow(
    redBalls: List<Int>,
    blueBalls: List<Int>,
    padTwoDigits: Boolean = true,
    ballSize: Dp = 42.dp
) {
    val total = redBalls.size + blueBalls.size
    val effectiveSize = when {
        total >= 15 -> 34.dp
        total >= 7 -> if (ballSize > 38.dp) 38.dp else ballSize
        else -> ballSize
    }
    val fontSize = when {
        total >= 15 -> 14.sp
        total >= 7 -> 16.sp
        else -> 18.sp
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        redBalls.forEach { num ->
            NumberBall(
                text = ballLabel(num, padTwoDigits),
                color = RedBallColor,
                size = effectiveSize,
                fontSize = fontSize
            )
        }
        // 前区 + 后区 分隔（大乐透等乐透型玩法）
        if (redBalls.isNotEmpty() && blueBalls.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .height(effectiveSize)
                    .width(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = MutedGray, fontSize = fontSize, fontWeight = FontWeight.Bold)
            }
        }
        blueBalls.forEach { num ->
            NumberBall(
                text = ballLabel(num, padTwoDigits),
                color = BlueBallColor,
                size = effectiveSize,
                fontSize = fontSize
            )
        }
    }
}

private fun ballLabel(num: Int, padTwoDigits: Boolean): String =
    if (padTwoDigits) num.toString().padStart(2, '0') else num.toString()

@Composable
fun NumberBall(
    text: String,
    color: Color,
    size: Dp = 42.dp,
    fontSize: TextUnit = 17.sp
) {
    // 立体感渐变：左上高光 → 本色 → 右下暗部
    val brush = remember(color) {
        Brush.linearGradient(
            listOf(
                lerp(color, Color.White, 0.30f),
                color,
                lerp(color, Color.Black, 0.18f)
            )
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 3.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/** 圆角状态标签，例如「今晚开奖」「已开奖」「新浪快讯」 */
@Composable
fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
