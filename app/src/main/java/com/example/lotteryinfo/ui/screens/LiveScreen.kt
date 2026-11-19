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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button

private data class OfficialLiveSource(
    val id: String,
    val label: String,
    val url: String
)

private val OFFICIAL_SOURCES = listOf(
    OfficialLiveSource("fc_live", "福彩官方直播", "https://www.cwl.gov.cn/html5/ygkj/kjzb/"),
OfficialLiveSource("tc_live", "体彩官方直播", "https://m.lottery.gov.cn/kjzb/")
)

// 注入到官网直播/往期页面的净化脚本：隐藏导航、广告、新闻、页脚二维码，只保留视频、倒计时与往期视频列表
private const val PURIFY_PAGE_JS = """
(function() {
  var css = `
  header, nav, footer, .app-guide, .download-app, .copyright { display: none !important; } html, body { margin: 0 !important; max-width: 100% !important; overflow-x: hidden !important; } video, .video-box, .player, .video-wrap, iframe { width: 100% !important; max-width: 100% !important; min-height: 240px !important; }
  `;
  var style = document.getElementById('apk-purify-style');
  if (!style) {
    style = document.createElement('style');
    style.id = 'apk-purify-style';
    document.head.appendChild(style);
  }
  style.innerHTML = css;
      document.querySelectorAll('a, div, section, button, span').forEach(function(el) { if (el.querySelector('video, iframe')) return; var t = (el.innerText || '').trim(); if (t.length > 0 && t.length < 300 && t.indexOf('APP专享') !== -1) { el.style.display = 'none'; } });
      
  // 确保页面内所有视频支持内联播放与控制条
  document.querySelectorAll('video').forEach(function(v) {
    v.setAttribute('controls', 'controls');
    v.setAttribute('playsinline', 'true');
    v.setAttribute('webkit-playsinline', 'true');
  });
})();
"""

private fun buildFullscreenJs(enable: Boolean): String = if (enable) "(function(){var s=document.getElementById('apk-fullscreen-style');if(!s){s=document.createElement('style');s.id='apk-fullscreen-style';document.head.appendChild(s);}s.innerHTML='video,.video-box,.live-box,.player-box,.video-wrap,.prism-player,.video-js,[id*=\"player\"],[id*=\"video\"],iframe{position:fixed!important;top:0!important;left:0!important;width:100vw!important;height:100vh!important;z-index:999999!important;background:#000!important;}video{width:100%!important;height:100%!important;object-fit:contain!important;}';})();" else "(function(){var s=document.getElementById('apk-fullscreen-style');if(s){s.remove();}})();"    

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
var loadError by remember { mutableStateOf<String?>(null) }
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
FilledTonalIconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeSource.url))) }, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFF2B2F36), contentColor = Color.White)) { Icon(Icons.Default.OpenInBrowser, contentDescription = "浏览器打开") }

                FilledTonalIconButton(
                    onClick = { webViewRef?.evaluateJavascript(buildFullscreenJs(true), null); onFullscreenChange(true) },
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
        if (!isFullscreen && activeSource.id == "tc_live") {
            var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while (true) {
                    nowMillis = System.currentTimeMillis()
                    delay(1000L)
                }
            }
            val tz = remember { TimeZone.getTimeZone("Asia/Shanghai") }
            val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val second = cal.get(Calendar.SECOND)
            val secondsSinceMidnight = hour * 3600 + minute * 60 + second
            val startLiveSec = 21 * 3600 + 20 * 60
            val endLiveSec = 21 * 3600 + 40 * 60
            val targetDrawSec = 21 * 3600 + 25 * 60
            val isLiveWindow = secondsSinceMidnight in startLiveSec..endLiveSec
            val bannerText = if (isLiveWindow) {
                "正在体彩开奖直播时段（21:25 开奖）"
            } else {
                val remainSec = if (secondsSinceMidnight < targetDrawSec) {
                    targetDrawSec - secondsSinceMidnight
                } else {
                    24 * 3600 - secondsSinceMidnight + targetDrawSec
                }
                val h = remainSec / 3600
                val m = (remainSec % 3600) / 60
                val s = remainSec % 60
                String.format(Locale.getDefault(), "体彩开奖倒计时：距下一次 21:25 开奖还剩 %02d:%02d:%02d", h, m, s)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isLiveWindow) Color(0xFFD32F2F) else Color(0xFF1F3A5F)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isLiveWindow) Color.White else Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bannerText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
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
                            textZoom = 100
                            displayZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
val url = request?.url?.toString() ?: return false; val lowerUrl = url.lowercase(); if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) return true; if (lowerUrl.contains(".apk")) return true; return false }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url?.contains("cwl.gov.cn", ignoreCase = true) == true) { view?.evaluateJavascript(PURIFY_PAGE_JS, null) } else if (url?.contains("lottery.gov.cn", ignoreCase = true) == true) { view?.evaluateJavascript("(function(){document.querySelectorAll('video').forEach(function(v){v.controls=true;v.setAttribute('controls','controls');v.setAttribute('playsinline','');v.setAttribute('webkit-playsinline','');});})();", null) }
                            } override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) { super.onReceivedError(view, request, error); if (request?.isForMainFrame == true) { loadError = "页面加载失败 (错误码: ${error?.errorCode ?: -1}, ${error?.description ?: "未知错误"})" } }
                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) { super.onReceivedHttpError(view, request, errorResponse); if (request?.isForMainFrame == true) { loadError = "HTTP 请求失败 (状态码: ${errorResponse?.statusCode ?: -1})" } }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) { super.onPageStarted(view, url, favicon); loadError = null }
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
        if (loadError != null) { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF14161A)).padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(text = loadError ?: "", color = Color.White, style = MaterialTheme.typography.bodyLarge); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = { loadError = null; webViewRef?.reload() }) { Text("重新加载") }; Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeSource.url))) }) { Text("用系统浏览器打开") } } } } }
            // 全屏模式下右上角保留退出全屏悬浮按钮
            if (isFullscreen) {
                FilledTonalIconButton(
                    onClick = {
                        if (customVideoView != null) exitVideoCustomView()
                       webViewRef?.evaluateJavascript(buildFullscreenJs(false), null)
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
