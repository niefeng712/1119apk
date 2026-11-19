package com.example.lotteryinfo.data

import com.example.lotteryinfo.model.LiveChannel
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.model.LotteryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FetchResult(
    val draws: List<LotteryDraw>,
    val isFromNetwork: Boolean,
    val statusMessage: String
)

object LotteryRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun getLiveChannels(): List<LiveChannel> = listOf(
        LiveChannel(
            id = "fc_official",
            title = "中国福彩官方开奖直播",
            org = LotteryOrg.FC,
            scheduleText = "每晚 21:15（双色球 / 福彩3D / 七乐彩）",
            webLiveUrl = LotteryConfig.FC_OFFICIAL_LIVE_WEB
        ),
        LiveChannel(
            id = "fc_sina",
            title = "新浪彩票 · 福彩直播与往期回放",
            org = LotteryOrg.FC,
            scheduleText = "支持实时开奖与往期视频回看",
            webLiveUrl = LotteryConfig.FC_SINA_LIVE_WEB
        ),
        LiveChannel(
            id = "tc_official",
            title = "中国体彩官方开奖直播",
            org = LotteryOrg.TC,
            scheduleText = "每晚 21:25（超级大乐透 / 排列三 / 排列五）",
            webLiveUrl = LotteryConfig.TC_OFFICIAL_LIVE_WEB
        ),
        LiveChannel(
            id = "tc_sina",
            title = "新浪彩票 · 体彩直播与往期回放",
            org = LotteryOrg.TC,
            scheduleText = "每日 21:00 起播，21:25 开奖",
            webLiveUrl = LotteryConfig.TC_SINA_LIVE_WEB
        )
    )

    suspend fun fetchHistoryByType(type: LotteryType, count: Int = 20): FetchResult =
        withContext(Dispatchers.IO) {
            try {
                val remote = when {
                    type.cwlName != null -> fetchFromCwl(type, type.cwlName, count)
                    type.sportteryGameNo != null -> fetchFromSporttery(type, type.sportteryGameNo, count)
                    else -> emptyList()
                }
                if (remote.isNotEmpty()) {
                    FetchResult(remote, isFromNetwork = true, statusMessage = "已同步官方公开接口实时数据")
                } else {
                    FetchResult(
                        getFallbackHistory(type),
                        isFromNetwork = false,
                        statusMessage = "接口返回为空，已切换本地示例数据"
                    )
                }
            } catch (e: Exception) {
                FetchResult(
                    getFallbackHistory(type),
                    isFromNetwork = false,
                    statusMessage = "网络受限 (${e.javaClass.simpleName})，当前显示本地示例数据"
                )
            }
        }

    suspend fun fetchLatestAll(): FetchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<LotteryDraw>()
        var anyNetwork = false
        for (type in LotteryType.entries) {
            val res = fetchHistoryByType(type, count = 1)
            if (res.isFromNetwork) anyNetwork = true
            res.draws.firstOrNull()?.let { results.add(it) }
        }
        val msg = if (anyNetwork) "已同步部分或全部官方最新开奖数据" else "当前显示离线示例数据（请检查网络）"
        FetchResult(results, isFromNetwork = anyNetwork, statusMessage = msg)
    }

    private fun fetchFromCwl(type: LotteryType, cwlName: String, count: Int): List<LotteryDraw> {
        val url = "${LotteryConfig.CWL_DRAW_API}?name=$cwlName&issueCount=$count&pageNo=1&pageSize=$count"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .header("Referer", "https://www.cwl.gov.cn/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val arr = json.optJSONArray("result") ?: return emptyList()
            val list = mutableListOf<LotteryDraw>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val issue = item.optString("code")
                val date = item.optString("date").substringBefore("(").trim()
                val redStr = item.optString("red")
                val blueStr = item.optString("blue")
                val reds = redStr.split(",", " ").mapNotNull { it.trim().toIntOrNull() }
                val blues = if (blueStr.isBlank()) emptyList()
                else blueStr.split(",", " ").mapNotNull { it.trim().toIntOrNull() }
                val pool = formatMoney(item.optString("poolmoney"))
                val sales = formatMoney(item.optString("sales"))
                var firstCount = 0
                var firstMoney = "--"
                val grades = item.optJSONArray("prizegrades")
                if (grades != null && grades.length() > 0) {
                    val g1 = grades.getJSONObject(0)
                    firstCount = g1.optString("typenum").toIntOrNull() ?: 0
                    firstMoney = formatMoney(g1.optString("typemoney"))
                }
                list.add(
                    LotteryDraw(
                        lotteryType = type,
                        issue = issue,
                        drawDate = date,
                        redBalls = reds,
                        blueBalls = blues,
                        poolAmount = pool,
                        salesAmount = sales,
                        firstPrizeCount = firstCount,
                        firstPrizeAmount = firstMoney,
                        isLiveData = true
                    )
                )
            }
            return list
        }
    }

    private fun fetchFromSporttery(type: LotteryType, gameNo: String, count: Int): List<LotteryDraw> {
        val url = "${LotteryConfig.SPORTTERY_DRAW_API}?gameNo=$gameNo&provinceId=0&pageSize=$count&isVerify=1&pageNo=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .header("Referer", "https://www.lottery.gov.cn/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val valueObj = json.optJSONObject("value") ?: return emptyList()
            val arr = valueObj.optJSONArray("list") ?: return emptyList()
            val list = mutableListOf<LotteryDraw>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val issue = item.optString("lotteryDrawNum")
                val date = item.optString("lotteryDrawTime")
                val rawNums = item.optString("lotteryDrawResult")
                    .split(" ", ",", "+")
                    .mapNotNull { it.trim().toIntOrNull() }
                val (reds, blues) = if (type == LotteryType.DLT && rawNums.size >= 7) {
                    rawNums.take(5) to rawNums.drop(5).take(2)
                } else {
                    rawNums to emptyList()
                }
                val pool = formatMoney(item.optString("poolBalanceAfterdraw").replace(",", ""))
                val sales = formatMoney(item.optString("totalSaleAmount").replace(",", ""))
                var firstCount = 0
                var firstMoney = "--"
                val prizes = item.optJSONArray("prizeLevelList")
                if (prizes != null && prizes.length() > 0) {
                    val p1 = prizes.getJSONObject(0)
                    firstCount = p1.optString("stakeCount").replace(",", "").toIntOrNull() ?: 0
                    firstMoney = formatMoney(p1.optString("stakeAmount").replace(",", ""))
                }
                list.add(
                    LotteryDraw(
                        lotteryType = type,
                        issue = issue,
                        drawDate = date,
                        redBalls = reds,
                        blueBalls = blues,
                        poolAmount = pool,
                        salesAmount = sales,
                        firstPrizeCount = firstCount,
                        firstPrizeAmount = firstMoney,
                        isLiveData = true
                    )
                )
            }
            return list
        }
    }

    private fun formatMoney(raw: String): String {
        val num = raw.trim().toDoubleOrNull() ?: return if (raw.isBlank()) "固定派奖" else raw
        return when {
            num >= 100_000_000 -> String.format("%.2f 亿元", num / 100_000_000.0)
            num >= 10_000 -> String.format("%.1f 万元", num / 10_000.0)
            num > 0 -> String.format("%.0f 元", num)
            else -> "固定派奖"
        }
    }

    fun getFallbackHistory(type: LotteryType): List<LotteryDraw> {
        return when (type) {
            LotteryType.SSQ -> listOf(
                LotteryDraw(LotteryType.SSQ, "2026132", "2026-11-17 (示例)", listOf(3, 8, 14, 19, 25, 31), listOf(7), "21.40 亿元", "3.80 亿元", 6, "812.0 万元"),
                LotteryDraw(LotteryType.SSQ, "2026131", "2026-11-15 (示例)", listOf(2, 11, 16, 22, 28, 33), listOf(12), "21.10 亿元", "4.10 亿元", 4, "940.0 万元"),
                LotteryDraw(LotteryType.SSQ, "2026130", "2026-11-12 (示例)", listOf(5, 9, 13, 20, 24, 30), listOf(4), "20.80 亿元", "3.70 亿元", 9, "685.0 万元"),
                LotteryDraw(LotteryType.SSQ, "2026129", "2026-11-10 (示例)", listOf(1, 7, 15, 18, 27, 32), listOf(15), "20.50 亿元", "3.90 亿元", 5, "860.0 万元"),
                LotteryDraw(LotteryType.SSQ, "2026128", "2026-11-08 (示例)", listOf(4, 10, 17, 21, 26, 29), listOf(9), "20.20 亿元", "4.00 亿元", 7, "750.0 万元"),
                LotteryDraw(LotteryType.SSQ, "2026127", "2026-11-05 (示例)", listOf(6, 12, 14, 23, 25, 33), listOf(2), "19.90 亿元", "3.60 亿元", 3, "1000.0 万元")
            )
            LotteryType.DLT -> listOf(
                LotteryDraw(LotteryType.DLT, "2026132", "2026-11-16 (示例)", listOf(4, 12, 18, 26, 34), listOf(3, 9), "9.80 亿元", "3.10 亿元", 3, "1000.0 万元"),
                LotteryDraw(LotteryType.DLT, "2026131", "2026-11-14 (示例)", listOf(2, 9, 15, 21, 30), listOf(5, 11), "9.50 亿元", "3.30 亿元", 2, "1000.0 万元"),
                LotteryDraw(LotteryType.DLT, "2026130", "2026-11-11 (示例)", listOf(7, 14, 20, 27, 35), listOf(1, 8), "9.20 亿元", "2.90 亿元", 5, "890.0 万元"),
                LotteryDraw(LotteryType.DLT, "2026129", "2026-11-09 (示例)", listOf(1, 11, 19, 24, 32), listOf(4, 12), "9.00 亿元", "3.00 亿元", 4, "960.0 万元")
            )
            LotteryType.FC3D -> listOf(
                LotteryDraw(LotteryType.FC3D, "2026310", "2026-11-17 (示例)", listOf(3, 7, 9), emptyList(), "固定派奖", "0.92 亿元", 0, "1040 元"),
                LotteryDraw(LotteryType.FC3D, "2026309", "2026-11-16 (示例)", listOf(1, 5, 8), emptyList(), "固定派奖", "0.89 亿元", 0, "1040 元"),
                LotteryDraw(LotteryType.FC3D, "2026308", "2026-11-15 (示例)", listOf(4, 4, 2), emptyList(), "固定派奖", "0.94 亿元", 0, "1040 元")
            )
            LotteryType.PL3 -> listOf(
                LotteryDraw(LotteryType.PL3, "2026310", "2026-11-17 (示例)", listOf(5, 2, 8), emptyList(), "固定派奖", "0.45 亿元", 0, "1040 元"),
                LotteryDraw(LotteryType.PL3, "2026309", "2026-11-16 (示例)", listOf(9, 1, 4), emptyList(), "固定派奖", "0.43 亿元", 0, "1040 元")
            )
            LotteryType.QLC -> listOf(
                LotteryDraw(LotteryType.QLC, "2026126", "2026-11-16 (示例)", listOf(4, 9, 15, 20, 23, 25, 30), listOf(7), "1200.0 万元", "680.0 万元", 1, "180.0 万元")
            )
        }
    }
}
