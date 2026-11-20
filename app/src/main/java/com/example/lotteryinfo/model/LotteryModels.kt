package com.example.lotteryinfo.model

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
    val sportteryGameNo: String?
) {
    FC3D("fc3d", "福彩3D", LotteryOrg.FC, "每日 21:15", 9, 0, true, 3, "3d", null),
    PL3("pl3", "排列三", LotteryOrg.TC, "每日 21:25", 9, 0, true, 3, null, "35"),
    PL5("pl5", "排列五", LotteryOrg.TC, "每日 21:25", 9, 0, true, 5, null, "350133")
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
