package com.example.lotteryinfo.data

import android.content.Context
import com.example.lotteryinfo.model.LiveChannel
import com.example.lotteryinfo.model.LotteryDraw
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.model.LotteryType
import com.example.lotteryinfo.model.SINA_SOURCE_MARK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FetchResult(
    val draws: List<LotteryDraw>,
    val isFromNetwork: Boolean,
    val statusMessage: String
)

object LotteryRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val fastLatestMap = mutableMapOf<LotteryType, LotteryDraw>()
  private const val PREF_NAME = "lottery_latest_cache"
private var appContext: Context? = null
private val syncTimeMap = mutableMapOf<LotteryType, String>()
private val syncDateMap = mutableMapOf<LotteryType, String>()
    fun normalizeIssue(issue: String): Long {
        val digits = issue.filter { it.isDigit() }
        val normalized = if (digits.length == 5) "20$digits" else digits
        return normalized.toLongOrNull() ?: 0L
    }

    private fun currentBeijingTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return sdf.format(Date())
    }
private fun currentBeijingDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return sdf.format(Date())
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
        synchronized(fastLatestMap) {
            LotteryType.entries.forEach { type ->
                val jsonStr = prefs.getString("latest_${type.name}", null)
                if (!jsonStr.isNullOrBlank()) {
                    runCatching {
                        val obj = JSONObject(jsonStr)
                        val reds = mutableListOf<Int>()
                        val redArr = obj.optJSONArray("redBalls") ?: JSONArray()
                        for (i in 0 until redArr.length()) reds.add(redArr.getInt(i))
                        val blues = mutableListOf<Int>()
                        val blueArr = obj.optJSONArray("blueBalls") ?: JSONArray()
                        for (i in 0 until blueArr.length()) blues.add(blueArr.getInt(i))
                        val draw = LotteryDraw(
                            lotteryType = type,
                            issue = obj.optString("issue"),
                            drawDate = obj.optString("drawDate"),
                            redBalls = reds,
                            blueBalls = blues,
                            poolAmount = obj.optString("poolAmount"),
                            salesAmount = obj.optString("salesAmount"),
                            firstPrizeCount = obj.optInt("firstPrizeCount", 0),
                            firstPrizeAmount = obj.optString("firstPrizeAmount"),
                            isLiveData = obj.optBoolean("isLiveData", true)
                        )
                        val cur = fastLatestMap[type]?.issue?.let { normalizeIssue(it) } ?: 0L
                        if (normalizeIssue(draw.issue) >= cur) {
                            fastLatestMap[type] = draw
                        }
                    }
                }
                prefs.getString("sync_time_${type.name}", null)?.let { syncTimeMap[type] = it }
                prefs.getString("sync_date_${type.name}", null)?.let { syncDateMap[type] = it }
            }
        }
    }

    fun updateFastLatest(draw: LotteryDraw, syncTime: String = "") {
        // 离线示例数据不写入缓存，避免污染“最新一期”
        if (draw.drawDate.contains("示例")) return
        val time = syncTime.ifBlank { currentBeijingTime() }
        val date = currentBeijingDate()
        synchronized(fastLatestMap) {
            val existing = fastLatestMap[draw.lotteryType]
            val cur = existing?.issue?.let { normalizeIssue(it) } ?: 0L
            val incoming = normalizeIssue(draw.issue)
            // 同一期：新浪快讯（只有号码）不能覆盖官方完整数据（奖池、销量、一等奖）
            if (existing != null && incoming == cur &&
                draw.poolAmount == SINA_SOURCE_MARK && existing.poolAmount != SINA_SOURCE_MARK
            ) {
                syncTimeMap[draw.lotteryType] = time
                syncDateMap[draw.lotteryType] = date
                appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()
                    ?.putString("sync_time_${draw.lotteryType.name}", time)
                    ?.putString("sync_date_${draw.lotteryType.name}", date)
                    ?.apply()
                return
            }
            if (incoming >= cur) {
                fastLatestMap[draw.lotteryType] = draw
                syncTimeMap[draw.lotteryType] = time
                syncDateMap[draw.lotteryType] = date
                val prefs = appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                if (prefs != null) {
                    val obj = JSONObject().apply {
                        put("issue", draw.issue)
                        put("drawDate", draw.drawDate)
                        put("redBalls", JSONArray(draw.redBalls))
                        put("blueBalls", JSONArray(draw.blueBalls))
                        put("poolAmount", draw.poolAmount)
                        put("salesAmount", draw.salesAmount)
                        put("firstPrizeCount", draw.firstPrizeCount)
                        put("firstPrizeAmount", draw.firstPrizeAmount)
                        put("isLiveData", draw.isLiveData)
                    }
                    prefs.edit()
                        .putString("latest_${draw.lotteryType.name}", obj.toString())
                        .putString("sync_time_${draw.lotteryType.name}", time)
                        .putString("sync_date_${draw.lotteryType.name}", date)
                        .apply()
                }
            }
        }
    }

    fun getSyncTime(type: LotteryType): String {
        synchronized(fastLatestMap) {
            return syncTimeMap[type] ?: appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                ?.getString("sync_time_${type.name}", "") ?: ""
        }
    }

    /** 北京时间今天是否为该彩种开奖日 */
    fun isDrawDayToday(type: LotteryType): Boolean {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
        return cal.get(java.util.Calendar.DAY_OF_WEEK) in type.drawDays
    }

    /** 今日无需再同步：今天不开奖，或今天的开奖已更新 */
    fun isDoneForToday(type: LotteryType): Boolean =
        !isDrawDayToday(type) || isUpdatedForToday(type)

    fun isUpdatedForToday(type: LotteryType): Boolean {
        synchronized(fastLatestMap) {
            val draw = fastLatestMap[type] ?: return false
            val todayFull = currentBeijingDate()
            val todayShort = todayFull.substring(5)
            return draw.drawDate.contains(todayFull) || draw.drawDate.contains(todayShort)
        }
    }

    fun getAllLatestSnapshot(): List<LotteryDraw> {
        synchronized(fastLatestMap) {
            return LotteryType.entries.map { type ->
                fastLatestMap[type] ?: getFallbackHistory(type).first()
            }
        }
    }
    
    
    fun getLiveChannels(): List<LiveChannel> = listOf(
        LiveChannel(
            id = "fc_official",
            title = "中国福彩官方直播",
            org = LotteryOrg.FC,
            scheduleText = "每晚 21:15（双色球/3D/七乐彩）· 21:30（快乐8）",
            webLiveUrl = LotteryConfig.FC_OFFICIAL_LIVE_WEB
        ),
        LiveChannel(
            id = "tc_official",
            title = "中国体彩官方直播",
            org = LotteryOrg.TC,
            scheduleText = "每晚 21:25（大乐透/排列三/排列五/七星彩）",
            webLiveUrl = LotteryConfig.TC_OFFICIAL_LIVE_WEB
        ),
        LiveChannel(
            id = "fc_sina",
            title = "新浪福彩（含往期回放）",
            org = LotteryOrg.FC,
            scheduleText = "支持实时开奖与往期开奖视频回放",
            webLiveUrl = LotteryConfig.FC_SINA_LIVE_WEB
        ),
        LiveChannel(
            id = "tc_sina",
            title = "新浪体彩（含往期回放）",
            org = LotteryOrg.TC,
            scheduleText = "每日 21:00 起播，支持往期回放",
            webLiveUrl = LotteryConfig.TC_SINA_LIVE_WEB
        )
    )

    suspend fun fetchHistoryByType(
        type: LotteryType,
        pageNo: Int = 1,
        pageSize: Int = 100
  ): FetchResult = withContext(Dispatchers.IO) {
        try {
           val timeStr = currentBeijingTime()
            if (pageNo == 1) {
                val officialList = try {
                    when {
                        type.cwlName != null -> fetchFromCwl(type, type.cwlName, pageNo, pageSize)
                        type.sportteryGameNo != null -> fetchFromSporttery(type, type.sportteryGameNo, pageNo, pageSize)
                        else -> emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                officialList.firstOrNull()?.let { updateFastLatest(it, timeStr) }
                val cachedTop = synchronized(fastLatestMap) { fastLatestMap[type] }
                if (cachedTop != null) {
                    val cachedNorm = normalizeIssue(cachedTop.issue)
                    val merged = listOf(cachedTop) + officialList.filterNot { normalizeIssue(it.issue) == cachedNorm }
                    return@withContext FetchResult(
                        draws = merged,
                        isFromNetwork = officialList.isNotEmpty(),
                        statusMessage = "已同步 ${cachedTop.issue}期 ($timeStr)"
                    )
                } else {
                    return@withContext FetchResult(
                        draws = getFallbackHistory(type),
                        isFromNetwork = false,
                        statusMessage = "接口暂无更多数据 ($timeStr)"
                    )
                }
            } else {
                val remote = when {
                    type.cwlName != null -> fetchFromCwl(type, type.cwlName, pageNo, pageSize)
                    type.sportteryGameNo != null -> fetchFromSporttery(type, type.sportteryGameNo, pageNo, pageSize)
                    else -> emptyList()
                }
                if (remote.isNotEmpty()) {
                    FetchResult(remote, isFromNetwork = true, statusMessage = "已同步官方公开接口 (${remote.size}期) ($timeStr)")
                } else {
                    FetchResult(emptyList(), isFromNetwork = false, statusMessage = "接口暂无更多数据 ($timeStr)")
                }
            }
        } catch (e: Exception) {
            FetchResult(
                if (pageNo == 1) getFallbackHistory(type) else emptyList(),
                isFromNetwork = false,
                statusMessage = "网络受限 (${e.javaClass.simpleName})，当前显示本地示例数据"
            )
        }
    }

    suspend fun fetchLatestAll(): FetchResult = coroutineScope {
        val deferred = LotteryType.entries.map { type ->
            async(Dispatchers.IO) {
                fetchHistoryByType(type, pageNo = 1, pageSize = 1)
            }
        }
        val allResults = deferred.awaitAll()
        val draws = allResults.mapNotNull { it.draws.firstOrNull() }
        val anyNetwork = allResults.any { it.isFromNetwork }
        val msg = if (anyNetwork) "已同步官方最新开奖数据" else "当前显示离线示例数据（请检查网络）"
        FetchResult(draws, isFromNetwork = anyNetwork, statusMessage = msg)
    }
  
  

    private fun fetchFromCwl(
        type: LotteryType,
        cwlName: String,
        pageNo: Int,
        pageSize: Int
    ): List<LotteryDraw> {
        val url = "${LotteryConfig.CWL_DRAW_API}?name=$cwlName&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd=&pageNo=$pageNo&pageSize=$pageSize&week=&systemType=PC&_t=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
            .header("Referer", "https://www.cwl.gov.cn/")
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
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



    private fun fetchFromSporttery(
        type: LotteryType,
        gameNo: String,
        pageNo: Int,
        pageSize: Int
    ): List<LotteryDraw> {
        val url = "${LotteryConfig.SPORTTERY_DRAW_API}?gameNo=$gameNo&provinceId=0&pageSize=$pageSize&isVerify=1&pageNo=$pageNo&_t=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
            .header("Referer", "https://www.lottery.gov.cn/")
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
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
                // 乐透型（大乐透）：前 redCount 个为前区红球，其后 blueCount 个为后区蓝球
                if (type.blueCount > 0) {
                    if (rawNums.size >= type.redCount + type.blueCount) {
                        list.add(
                            buildSportteryDraw(
                                type, item, issue, date,
                                rawNums.take(type.redCount),
                                rawNums.drop(type.redCount).take(type.blueCount)
                            )
                        )
                    }
                    continue
                }
                val (reds, blues) = rawNums to emptyList<Int>()
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

    /** 构造体彩开奖记录（奖池、销量、一等奖字段与数字型玩法解析方式一致） */
    private fun buildSportteryDraw(
        type: LotteryType,
        item: JSONObject,
        issue: String,
        date: String,
        reds: List<Int>,
        blues: List<Int>
    ): LotteryDraw {
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
        return LotteryDraw(
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
           LotteryType.FC3D -> listOf(
                LotteryDraw(LotteryType.FC3D, "2026310", "2026-11-17 (示例)", listOf(3, 7, 9), emptyList(), "固定派奖", "0.92 亿元", 0, "1040 元"),
                LotteryDraw(LotteryType.FC3D, "2026309", "2026-11-16 (示例)", listOf(1, 5, 8), emptyList(), "固定派奖", "0.89 亿元", 0, "1040 元")
            )
            LotteryType.PL3 -> listOf(
                LotteryDraw(LotteryType.PL3, "2026310", "2026-11-17 (示例)", listOf(5, 2, 8), emptyList(), "固定派奖", "0.45 亿元", 0, "1040 元"),
                LotteryDraw(LotteryType.PL3, "2026309", "2026-11-16 (示例)", listOf(9, 1, 4), emptyList(), "固定派奖", "0.43 亿元", 0, "1040 元")
            )
            LotteryType.PL5 -> listOf(
                LotteryDraw(LotteryType.PL5, "2026310", "2026-11-17 (示例)", listOf(3, 6, 2, 9, 1), emptyList(), "固定派奖", "0.21 亿元", 12, "10.0 万元")
         )
            LotteryType.DLT -> listOf(
                LotteryDraw(LotteryType.DLT, "26133", "2026-11-21 (示例)", listOf(10, 11, 25, 30, 32), listOf(9, 10), "--", "--", 0, "--")
            )
        }
    }
}
