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
    val cwlName: String?,
    val sportteryGameNo: String?
) {
    SSQ("ssq", "双色球", LotteryOrg.FC, "每周二、四、日 21:15", 33, 16, "ssq", null),
    DLT("dlt", "超级大乐透", LotteryOrg.TC, "每周一、三、六 21:25", 35, 12, null, "85"),
    FC3D("fc3d", "福彩3D", LotteryOrg.FC, "每日 21:15", 9, 0, "3d", null),
    PL3("pl3", "排列三", LotteryOrg.TC, "每日 21:25", 9, 0, null, "35"),
    QLC("qlc", "七乐彩", LotteryOrg.FC, "每周一、三、五 21:15", 30, 30, "qlc", null)
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
    val webLiveUrl: String,
    val directStreamUrl: String = ""
)
