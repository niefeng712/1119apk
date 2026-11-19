package com.example.lotteryinfo.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.lotteryinfo.data.VideoEpisode
import com.example.lotteryinfo.data.VideoRepository
import com.example.lotteryinfo.ui.layout.ScreenSizeClass

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun LiveScreen(
    sizeClass: ScreenSizeClass,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isWelfare by remember { mutableStateOf(true) }
    var episodes by remember { mutableStateOf<List<VideoEpisode>>(emptyList()) }
    var activeEpisode by remember { mutableStateOf<VideoEpisode?>(null) }
    var detectedStreamUrl by remember { mutableStateOf<String?>(null) }
    var sniffTargetUrl by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("正在提取开奖直播与往期视频...") }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }; var sniffTimedOut by remember { mutableStateOf(false) }; var countdownText by remember { mutableStateOf("00:00:00") }; LaunchedEffect(isWelfare) { while (true) { val now = java.util.Calendar.getInstance(); val target = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 21); set(java.util.Calendar.MINUTE, if (isWelfare) 15 else 25); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0); if (!after(now)) add(java.util.Calendar.DAY_OF_MONTH, 1) }; val diff = ((target.timeInMillis - now.timeInMillis) / 1000L).coerceAtLeast(0L); countdownText = String.format("%02d:%02d:%02d", diff / 3600L, (diff % 3600L) / 60L, diff % 60L); kotlinx.coroutines.delay(1000L) } }; LaunchedEffect(sniffTargetUrl, detectedStreamUrl, isLoading) { if (!isLoading && detectedStreamUrl == null && !sniffTargetUrl.isNullOrBlank()) { sniffTimedOut = false; kotlinx.coroutines.delay(6000L); if (detectedStreamUrl == null) { sniffTimedOut = true; statusMessage = "当前非开奖时段，可点击右侧列表中的视频测试播放与全屏" } } else { sniffTimedOut = false } }

    val dataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Pad) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                statusMessage = "流媒体播放受限，可点右上角刷新重试"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 1. 拉取直播与往期视频目录
    LaunchedEffect(isWelfare, refreshKey) {
        isLoading = true
        detectedStreamUrl = null
        statusMessage = "正在获取视频列表..."
        val list = VideoRepository.fetchVideoList(isWelfare)
        episodes = list
        activeEpisode = list.firstOrNull()
        isLoading = false
    }

    // 2. 切换视频时先尝试直解析，若无直链则启动后台隐身嗅探实时带 Token 的 .m3u8/.mp4
    LaunchedEffect(activeEpisode) {
        val ep = activeEpisode ?: return@LaunchedEffect
        isLoading = true
        detectedStreamUrl = null
        val direct = VideoRepository.resolveStreamUrl(ep)
        if (!direct.isNullOrBlank()) {
            detectedStreamUrl = direct
        } else {
            statusMessage = "正在后台解析实时视频流..."
            sniffTargetUrl = ep.pageUrl
        }
        isLoading = false
    }

    // 3. 拿到流地址后喂给纯原生 ExoPlayer 播放
    LaunchedEffect(detectedStreamUrl) {
        val url = detectedStreamUrl ?: return@LaunchedEffect
        val ep = activeEpisode
        val cookie = CookieManager.getInstance().getCookie(ep?.pageUrl ?: url) ?: ""
        val headers = buildMap {
            put("Referer", ep?.referer ?: "https://lotto.sina.cn/")
            if (cookie.isNotBlank()) put("Cookie", cookie)
        }
        dataSourceFactory.setDefaultRequestProperties(headers)
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        statusMessage = "正在播放：${ep?.title ?: "开奖视频"}"
        sniffTargetUrl = null
    }

    // 控制全屏系统栏
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
            else controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        onFullscreenChange(false)
    }

    // 后台 1dp 隐身嗅探器：不显示任何官网页面，只负责截获官网 JS 生成的带 Token .m3u8/.mp4 请求
    if (!sniffTargetUrl.isNullOrBlank()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 14; Pad) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: return null
                            if (reqUrl.contains(".m3u8", ignoreCase = true) ||
                                reqUrl.contains(".mp4", ignoreCase = true)
                            ) {
                                view?.post {
                                    if (detectedStreamUrl == null) {
                                        detectedStreamUrl = reqUrl
                                    }
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    loadUrl(sniffTargetUrl!!)
                }
            },
            update = { webView ->
                if (webView.url != sniffTargetUrl) {
                    webView.loadUrl(sniffTargetUrl!!)
                }
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
        )
    }

    val playerBox: @Composable (Modifier) -> Unit = { mod ->
        Box(
            modifier = mod
                .clip(if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (sniffTimedOut) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) { Text("距离下一次开奖直播还剩 $countdownText", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center); Spacer(Modifier.height(8.dp)); Text("可点击右侧列表中的视频测试播放与全屏", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } } else if (isLoading || (detectedStreamUrl == null && !sniffTargetUrl.isNullOrBlank())) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = statusMessage, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }

            FilledTonalIconButton(
                onClick = { onFullscreenChange(!isFullscreen) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "退出全屏" else "全屏播放"
                )
            }
        }
    }

    if (isFullscreen) {
        playerBox(Modifier.fillMaxSize())
        return
    }

    val listPanel: @Composable (Modifier) -> Unit = { mod ->
        Column(modifier = mod) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isWelfare,
                        onClick = { isWelfare = true },
                        modifier = Modifier.weight(1f), label = { Text("福彩视频", maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = !isWelfare,
                        onClick = { isWelfare = false },
                        modifier = Modifier.weight(1f), label = { Text("体彩视频", maxLines = 1, softWrap = false) }
                    )
                }
                FilledTonalIconButton(onClick = { refreshKey++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(episodes) { ep ->
                    val selected = ep.id == activeEpisode?.id
                    Card(
                        onClick = { activeEpisode = ep },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ep.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "播放",
                                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) { val isLandscapeWide = maxWidth > maxHeight && maxWidth >= 600.dp; if (isLandscapeWide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            playerBox(Modifier.weight(0.65f).fillMaxHeight())
            listPanel(Modifier.weight(0.35f).fillMaxHeight())
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            playerBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            listPanel(Modifier.weight(1f).fillMaxWidth())
        }
    }
    }
}
