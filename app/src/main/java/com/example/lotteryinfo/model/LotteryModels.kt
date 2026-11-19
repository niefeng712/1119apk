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
    SSQ("ssq", "双色球", LotteryOrg.FC, "每周二、四、日 21:15", 33, 16, false, 6, "ssq", null),
    DLT("dlt", "超级大乐透", LotteryOrg.TC, "每周一、三、六 21:25", 35, 12, false, 5, null, "85"),
    KL8("kl8", "快乐8", LotteryOrg.FC, "每日 21:30", 80, 0, false, 20, "kl8", null),
    FC3D("fc3d", "福彩3D", LotteryOrg.FC, "每日 21:15", 9, 0, true, 3, "3d", null),
    PL3("pl3", "排列三", LotteryOrg.TC, "每日 21:25", 9, 0, true, 3, null, "35"),
    PL5("pl5", "排列五", LotteryOrg.TC, "每日 21:25", 9, 0, true, 5, null, "350133"),
    QXC("qxc", "七星彩", LotteryOrg.TC, "每周二、五、日 21:25", 9, 14, true, 6, null, "04"),
    QLC("qlc", "七乐彩", LotteryOrg.FC, "每周一、三、五 21:15", 30, 30, false, 7, "qlc", null)
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
