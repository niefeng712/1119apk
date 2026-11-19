package com.example.lotteryinfo.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.lotteryinfo.data.LotteryRepository
import com.example.lotteryinfo.model.LiveChannel
import com.example.lotteryinfo.ui.layout.ScreenSizeClass

@Composable
fun LiveScreen(
    sizeClass: ScreenSizeClass,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val channels = remember { LotteryRepository.getLiveChannels() }
    var activeChannel by remember { mutableStateOf(channels.first()) }
    var useCustomStream by remember { mutableStateOf(false) }
    var customStreamUrl by remember { mutableStateOf("") }

    // 控制系统状态栏/导航栏隐藏，实现平板与手机真正全屏
    DisposableEffect(isFullscreen) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = (context as? Activity)?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        onFullscreenChange(false)
    }

    if (isFullscreen) {
        PlayerSection(
            activeChannel = activeChannel,
            useCustomStream = useCustomStream,
            customStreamUrl = customStreamUrl,
            isFullscreen = true,
            onToggleFullscreen = { onFullscreenChange(false) },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (sizeClass == ScreenSizeClass.EXPANDED) {
        // 9寸–14寸宽屏平板：左右双栏布局（左侧大屏播放器，右侧频道与模式控制）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                PlayerSection(
                    activeChannel = activeChannel,
                    useCustomStream = useCustomStream,
                    customStreamUrl = customStreamUrl,
                    isFullscreen = false,
                    onToggleFullscreen = { onFullscreenChange(true) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    LiveModeSelector(
                        useCustomStream = useCustomStream,
                        onToggleMode = { useCustomStream = it },
                        customStreamUrl = customStreamUrl,
                        onUrlChange = { customStreamUrl = it }
                    )
                }
                item {
                    Text(
                        text = "官方直播频道",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(channels) { channel ->
                    ChannelCard(
                        channel = channel,
                        selected = !useCustomStream && channel.id == activeChannel.id,
                        onClick = {
                            useCustomStream = false
                            activeChannel = channel
                        }
                    )
                }
            }
        }
    } else {
        // 手机与竖屏小平板：上下布局
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PlayerSection(
                    activeChannel = activeChannel,
                    useCustomStream = useCustomStream,
                    customStreamUrl = customStreamUrl,
                    isFullscreen = false,
                    onToggleFullscreen = { onFullscreenChange(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            }
            item {
                LiveModeSelector(
                    useCustomStream = useCustomStream,
                    onToggleMode = { useCustomStream = it },
                    customStreamUrl = customStreamUrl,
                    onUrlChange = { customStreamUrl = it }
                )
            }
            items(channels) { channel ->
                ChannelCard(
                    channel = channel,
                    selected = !useCustomStream && channel.id == activeChannel.id,
                    onClick = {
                        useCustomStream = false
                        activeChannel = channel
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveModeSelector(
    useCustomStream: Boolean,
    onToggleMode: (Boolean) -> Unit,
    customStreamUrl: String,
    onUrlChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !useCustomStream,
                onClick = { onToggleMode(false) },
                label = { Text("官方直播网页") }
            )
            FilterChip(
                selected = useCustomStream,
                onClick = { onToggleMode(true) },
                label = { Text("直连流媒体 (m3u8)") }
            )
        }
        if (useCustomStream) {
            OutlinedTextField(
                value = customStreamUrl,
                onValueChange = onUrlChange,
                label = { Text("输入 .m3u8 或 .mp4 直播流地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
private fun PlayerSection(
    activeChannel: LiveChannel,
    useCustomStream: Boolean,
    customStreamUrl: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var webCustomView by remember { mutableStateOf<View?>(null) }
    var webCustomCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    LaunchedEffect(useCustomStream, customStreamUrl) {
        if (useCustomStream && customStreamUrl.isNotBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(customStreamUrl.trim()))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webCustomCallback?.onCustomViewHidden()
            exoPlayer.release()
        }
    }

    val shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF121417)),
        contentAlignment = Alignment.Center
    ) {
        if (useCustomStream) {
            if (customStreamUrl.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setFullscreenButtonClickListener { onToggleFullscreen() }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "请输入有效的 .m3u8 / .mp4 流媒体地址",
                    color = Color(0xFFBDC1C6),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                webCustomView = view
                                webCustomCallback = callback
                                onToggleFullscreen()
                            }

                            override fun onHideCustomView() {
                                webCustomView = null
                                webCustomCallback?.onCustomViewHidden()
                                webCustomCallback = null
                                if (isFullscreen) onToggleFullscreen()
                            }
                        }
                        webViewClient = WebViewClient()
                        loadUrl(activeChannel.webLiveUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != activeChannel.webLiveUrl) {
                        webView.loadUrl(activeChannel.webLiveUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 网页内嵌视频全屏自定义视图层
            if (webCustomView != null) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            (webCustomView?.parent as? ViewGroup)?.removeView(webCustomView)
                            addView(
                                webCustomView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 右上角全屏/退出全屏悬浮按钮（网页与播放器模式均可用）
        FilledTonalIconButton(
            onClick = {
                if (webCustomView != null) {
                    webCustomView = null
                    webCustomCallback?.onCustomViewHidden()
                    webCustomCallback = null
                }
                onToggleFullscreen()
            },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.55f),
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

@Composable
private fun ChannelCard(
    channel: LiveChannel,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = channel.scheduleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = if (selected) "播放中" else "切换",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
