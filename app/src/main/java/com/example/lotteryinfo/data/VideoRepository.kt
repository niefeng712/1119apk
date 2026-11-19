package com.example.lotteryinfo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class VideoEpisode(
    val id: String,
    val title: String,
    val category: String,
    val pageUrl: String,
    val streamUrl: String?,
    val referer: String
)

object VideoRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val streamRegex = Regex("""(?:https?:)?//[^"'\s<>]+?\.(?:m3u8|mp4)(?:\?[^"'\s<>]*)?""", RegexOption.IGNORE_CASE)
    private val sinaLinkRegex = Regex("""href=["']((?:https?:)?//lotto\.sina\.cn/[^"']+)["'][^>]*>([^<]*开奖[^<]*)<""")
    private fun normalizeUrl(raw: String): String = if (raw.startsWith("//")) "https:$raw" else raw
    
    suspend fun fetchVideoList(isWelfare: Boolean): List<VideoEpisode> = withContext(Dispatchers.IO) {
        val pageUrl = if (isWelfare) LotteryConfig.FC_SINA_LIVE_WEB else LotteryConfig.TC_SINA_LIVE_WEB
        val category = if (isWelfare) "中国福彩" else "中国体彩"
        val results = mutableListOf<VideoEpisode>()

        try {
            val html = fetchHtml(pageUrl, pageUrl)
            if (html.isNotBlank()) {
                // 提取主直播流地址 (.m3u8 / .mp4)
                val mainStreams = streamRegex.findAll(html).map { normalizeUrl(it.value) }.distinct().toList()
                if (mainStreams.isNotEmpty()) {
                    results.add(
                        VideoEpisode(
                            id = "${category}_live",
                            title = "$category 现场开奖直播",
                            category = category,
                            pageUrl = pageUrl,
                            streamUrl = mainStreams.first(),
                            referer = pageUrl
                        )
                    )
                }

                // 提取往期开奖视频页面链接
                val pastLinks = sinaLinkRegex.findAll(html).take(12).toList()
                pastLinks.forEachIndexed { idx, match ->
                    val link = normalizeUrl(match.groupValues[1])
                    val title = match.groupValues[2].trim().ifBlank { "$category 往期开奖视频 #${idx + 1}" }
                    results.add(
                        VideoEpisode(
                            id = "${category}_past_$idx",
                            title = title,
                            category = category,
                            pageUrl = link,
                            streamUrl = null,
                            referer = pageUrl
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (results.isEmpty()) {
            results.add(
                VideoEpisode(
                    id = "${category}_default",
                    title = "$category 开奖直播（待解析流地址）",
                    category = category,
                    pageUrl = pageUrl,
                    streamUrl = null,
                    referer = pageUrl
                )
            )
        }
        if (results.none { !it.streamUrl.isNullOrBlank() }) { results.add(VideoEpisode(id = "${category}_demo_replay", title = "往期开奖回放演示（可用于测试全屏与横竖屏）", category = category, pageUrl = pageUrl, streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", referer = pageUrl)) }
        results
    }

    suspend fun resolveStreamUrl(episode: VideoEpisode): String? = withContext(Dispatchers.IO) {
        if (!episode.streamUrl.isNullOrBlank()) return@withContext episode.streamUrl
        try {
            val html = fetchHtml(episode.pageUrl, episode.referer)
            streamRegex.find(html)?.value?.let { normalizeUrl(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchHtml(url: String, referer: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pad) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
            .header("Referer", referer)
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return ""
            return resp.body?.string() ?: ""
        }
    }
}
