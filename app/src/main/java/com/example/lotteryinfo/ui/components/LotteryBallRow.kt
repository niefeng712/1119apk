package com.example.lotteryinfo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryinfo.ui.theme.BlueBallColor
import com.example.lotteryinfo.ui.theme.RedBallColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LotteryBallRow(
    redBalls: List<Int>,
    blueBalls: List<Int>,
    padTwoDigits: Boolean = true,
    ballSize: Dp = 36.dp
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        redBalls.forEach { num ->
            NumberBall(
                text = if (padTwoDigits) num.toString().padStart(2, '0') else num.toString(),
                color = RedBallColor,
                size = ballSize
            )
        }
        blueBalls.forEach { num ->
            NumberBall(
                text = if (padTwoDigits) num.toString().padStart(2, '0') else num.toString(),
                color = BlueBallColor,
                size = ballSize
            )
        }
    }
}

@Composable
fun NumberBall(text: String, color: Color, size: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
