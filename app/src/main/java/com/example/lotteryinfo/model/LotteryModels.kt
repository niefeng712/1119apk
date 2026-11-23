package com.example.lotteryinfo.model

import java.util.Calendar

/** 每天开奖（周日..周六） */
private val EVERY_DAY = setOf(
    Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
)

enum class LotteryOrg(val label: String) {
    ALL("全部"),
    FC("中国福彩"),
    TC("中国体彩")
}

enum class LotteryType(
    val id: String,
    val displayName: String,
    val org: LotteryOrg,
    val drawSchedule: String,
    val redMax: Int,
    val blueMax: Int,
    val isDigitGame: Boolean,
    val digitCount: Int,
    val cwlName: String?,
    val sportteryGameNo: String?,
    /** 开奖的星期（Calendar.DAY_OF_WEEK） */
    val drawDays: Set<Int> = EVERY_DAY,
    /** 前区（红球）个数，仅乐透型玩法使用 */
    val redCount: Int = 0,
    /** 后区（蓝球）个数，仅乐透型玩法使用 */
    val blueCount: Int = 0
) {
    FC3D("fc3d", "福彩3D", LotteryOrg.FC, "每日 21:15", 9, 0, true, 3, "3d", null),
    PL3("pl3", "排列三", LotteryOrg.TC, "每日 21:25", 9, 0, true, 3, null, "35"),
    PL5("pl5", "排列五", LotteryOrg.TC, "每日 21:25", 9, 0, true, 5, null, "350133"),
    // 超级大乐透：前区 35 选 5 + 后区 12 选 2，每周一、三、六开奖
    DLT(
        "dlt", "大乐透", LotteryOrg.TC, "每周一、三、六 21:25", 35, 12, false, 0, null, "85",
        drawDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.SATURDAY),
        redCount = 5,
        blueCount = 2
    )
}

data class LotteryDraw(
    val lotteryType: LotteryType,
    val issue: String,
    val drawDate: String,
    val redBalls: List<Int>,
    val blueBalls: List<Int>,
    val poolAmount: String,
    val salesAmount: String,
    val firstPrizeCount: Int,
    val firstPrizeAmount: String,
    val isLiveData: Boolean = false
)

data class LiveChannel(
    val id: String,
    val title: String,
    val org: LotteryOrg,
    val scheduleText: String,
    val webLiveUrl: String
)
