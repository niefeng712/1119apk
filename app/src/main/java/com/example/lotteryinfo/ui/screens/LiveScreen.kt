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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lotteryinfo.ui.layout.ScreenSizeClass

private data class OfficialLiveSource(
    val id: String,
    val label: String,
    val url: String
)

private val OFFICIAL_SOURCES = listOf(
    OfficialLiveSource("fc_live", "福彩直播", "https://lotto.sina.cn/video/fcopen/"),
    OfficialLiveSource("tc_live", "体彩直播", "https://lotto.sina.cn/video/tcopen"),
    OfficialLiveSource("fc_past", "福彩往期", "https://www.zhcw.com/spzb/kjspzb/ssq/"),
    OfficialLiveSource("tc_past", "体彩往期", "https://sports.163.com/special/ticaiopen/")
)

// 注入到官网直播/往期页面的净化脚本：隐藏导航、广告、新闻、页脚二维码，只保留视频、倒计时与往期视频列表
private const val PURIFY_PAGE_JS = """
(function() {
  var css = `
    header, nav, footer, .header, .nav, .footer, .top-bar, .topbar, .bottom-bar,
    .ad, .ads, .advert, .banner, .qr, .qrcode, .ewm, .share, .comment, .comments,
    .news, .related-news, .hot-news, .recommend, .breadcrumbs, .position,
    aside, .sidebar, .right-bar, .copyright, .login-box, .download-app {
      display: none !important;
    }
    html, body {
      margin: 0 !important;
      padding: 6px !important;
      background: #111315 !important;
      color: #F5F7FA !important;
      font-size: 17px !important;
      font-weight: 600 !important;
      max-width: 100% !important;
      overflow-x: hidden !important;
    }
    video, .video-box, .player, .video-wrap, iframe {
      width: 100% !important;
      max-width: 100% !important;
      min-height: 240px !important;
    }
    a, span, p, li, div {
      font-size: 16px !important;
      line-height: 1.5 !important;
    }
  `;
  var style = document.getElementById('apk-purify-style');
  if (!style) {
    style = document.createElement('style');
    style.id = 'apk-purify-style';
    document.head.appendChild(style);
  }
  style.innerHTML = css;

  // 确保页面内所有视频支持内联播放与控制条
  document.querySelectorAll('video').forEach(function(v) {
    v.setAttribute('controls', 'controls');
    v.setAttribute('playsinline', 'true');
    v.setAttribute('webkit-playsinline', 'true');
  });
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveScreen(
    sizeClass: ScreenSizeClass,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var activeSource by remember { mutableStateOf(OFFICIAL_SOURCES.first()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun exitVideoCustomView() {
        val decor = activity?.window?.decorView as? FrameLayout
        customVideoView?.let { decor?.removeView(it) }
        customVideoView = null
        customVideoCallback?.onCustomViewHidden()
        customVideoCallback = null
    }

    // 控制系统状态栏与导航栏显示/隐藏
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111315))
    ) {
        // 顶部等宽单行切换栏（全屏时自动隐藏）
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1E22))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OFFICIAL_SOURCES.forEach { source ->
                    FilterChip(
                        selected = source.id == activeSource.id,
                        onClick = { activeSource = source },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF2B2F36),
                            labelColor = Color.White,
                            selectedContainerColor = Color(0xFFC5221F),
                            selectedLabelColor = Color.White
                        ),
                        label = {
                            Text(
                                text = source.label,
                                fontSize = if (sizeClass == ScreenSizeClass.COMPACT) 13.sp else 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                FilledTonalIconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFF2B2F36),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }

                FilledTonalIconButton(
                    onClick = { onFullscreenChange(true) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFF2B2F36),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "全屏播放")
                }
            }
        }

        // 官方直播/往期视频净化容器（保持单个 WebView 实例不被销毁）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    view?.loadUrl(url)
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(PURIFY_PAGE_JS, null)
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

                        tag = activeSource.id
                        loadUrl(activeSource.url)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    if (webView.tag != activeSource.id) {
                        webView.tag = activeSource.id
                        webView.loadUrl(activeSource.url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 全屏模式下右上角保留退出全屏悬浮按钮
            if (isFullscreen) {
                FilledTonalIconButton(
                    onClick = {
                        if (customVideoView != null) exitVideoCustomView()
                        onFullscreenChange(false)
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "退出全屏")
                }
            }
        }
    }
}
