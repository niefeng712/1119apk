package com.example.lotteryinfo.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lotteryinfo.data.LotteryRepository

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveScreen(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val channels = remember { LotteryRepository.getLiveChannels() }
    var activeChannel by remember { mutableStateOf(channels.first()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 记录 HTML5 视频全屏自定义视图（挂载到 Activity 根窗口，避免 Compose 重组导致白屏）
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun exitVideoCustomView() {
        val decor = activity?.window?.decorView as? FrameLayout
        customVideoView?.let { decor?.removeView(it) }
        customVideoView = null
        customVideoCallback?.onCustomViewHidden()
        customVideoCallback = null
    }

    // 控制系统状态栏和导航栏显示/隐藏
    DisposableEffect(isFullscreen) {
        val window = activity?.window
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
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exitVideoCustomView()
            webViewRef?.destroy()
        }
    }

    BackHandler(enabled = isFullscreen || customVideoView != null || webViewRef?.canGoBack() == true) {
        when {
            customVideoView != null -> {
                exitVideoCustomView()
                onFullscreenChange(false)
            }
            isFullscreen -> onFullscreenChange(false)
            webViewRef?.canGoBack() == true -> webViewRef?.goBack()
        }
    }

    // 纯净直播画面容器（保持单个 WebView 实例不被重建，彻底解决全屏白屏与往期视频无法播放问题）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false)
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 14; Pad) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            // 所有往期开奖视频与页面跳转均在当前 WebView 内打开，防止外跳失效
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                view?.loadUrl(url)
                                return true
                            }
                            return false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view == null) return
                            exitVideoCustomView()
                            customVideoView = view
                            customVideoCallback = callback
                            val decor = activity?.window?.decorView as? FrameLayout
                            decor?.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                            onFullscreenChange(true)
                        }

                        override fun onHideCustomView() {
                            exitVideoCustomView()
                            onFullscreenChange(false)
                        }
                    }

                    loadUrl(activeChannel.webLiveUrl)
                    webViewRef = this
                }
            },
            update = { webView ->
                if (webView.tag != activeChannel.id) {
                    webView.tag = activeChannel.id
                    webView.loadUrl(activeChannel.webLiveUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部悬浮精简频道切换栏（全屏时自动隐藏，留出纯享直播画面）
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 10.dp, end = 104.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                channels.forEach { channel ->
                    FilterChip(
                        selected = channel.id == activeChannel.id,
                        onClick = { activeChannel = channel },
                        label = { Text(channel.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            labelColor = Color.White,
                            selectedContainerColor = Color(0xFFC5221F),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 右上角刷新与全屏/退出全屏按钮
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isFullscreen) {
                FilledTonalIconButton(
                    onClick = { webViewRef?.reload() },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新直播页")
                }
            }

            FilledTonalIconButton(
                onClick = {
                    if (customVideoView != null) {
                        exitVideoCustomView()
                    }
                    onFullscreenChange(!isFullscreen)
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "退出全屏" else "全屏播放"
                )
            }
        }
    }
}
