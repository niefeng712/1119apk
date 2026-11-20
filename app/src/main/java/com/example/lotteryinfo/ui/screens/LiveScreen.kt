package com.example.lotteryinfo.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import com.example.lotteryinfo.model.LotteryOrg; import kotlinx.coroutines.delay; import java.util.Calendar; import java.util.TimeZone

private data class OfficialLiveSource(
    val id: String,
    val label: String,
    val url: String
)

private val OFFICIAL_SOURCES = listOf(
    OfficialLiveSource("fc_live", "福彩官方直播", "https://www.cwl.gov.cn/html5/ygkj/kjzb/"),
    OfficialLiveSource("tc_live", "体彩官方直播", "https://m.lottery.gov.cn/kjzb/")
)

private const val CWL_PURIFY_JS = """
        (function() { function purifyCwl() { var styleId = 'cwl-purify-style'; if (!document.getElementById(styleId)) { var style = document.createElement('style'); style.id = styleId; style.textContent = 'html, body { width: 100% !important; max-width: 100% !important; margin: 0 !important; padding: 0 !important; overflow-x: hidden !important; margin-top: 0 !important; padding-top: 0 !important; } body { padding-top: 64px !important; box-sizing: border-box !important; }'; (document.head || document.documentElement).appendChild(style); } var hideTabs = ['开奖公告', '往期开奖公告', '往期开奖视频', '开奖流程', '走进开奖现场', '开奖信息发布渠道']; var allEls = document.querySelectorAll('header, nav, footer, section, form, ul, li,p,  a, span, button, div'); for (var i = 0; i < allEls.length; i++) { var el = allEls[i]; if (el === document.body || el === document.documentElement) continue; if (el.querySelector('video, iframe')) continue; var txt = (el.innerText || el.textContent || '').trim(); if ((txt.indexOf('双色球：') !== -1 || txt.indexOf('快乐8：') !== -1 || txt.indexOf('七乐彩：') !== -1 || txt.indexOf('福彩3D：') !== -1) && txt.length < 500) { el.style.display = 'none'; continue; } if (hideTabs.indexOf(txt) !== -1 && el.children.length <= 2) { el.style.display = 'none'; continue; } if (txt === '开奖直播' && el.children.length <= 3) { el.style.display = 'none'; continue; } if ((txt.indexOf('推荐使用最新版') !== -1 || txt.indexOf('360浏览器') !== -1 || txt.indexOf('鸿蒙用户') !== -1) && txt.length < 500) { el.style.display = 'none'; continue; } if (txt.indexOf('开奖直播') === -1) { if ((txt.indexOf('客服热线') !== -1 || txt.indexOf('954168') !== -1 || txt.indexOf('中国福利彩票官方网站') !== -1) && txt.length < 300) { el.style.display = 'none'; continue; } var hasSearchBtn = (txt.indexOf('搜索') !== -1) || !!el.querySelector('input[value*="搜索"], button, [type="submit"], [type="button"]'); if (el.querySelector('input') && hasSearchBtn && txt.length < 150) { el.style.display = 'none'; continue; } if (txt.indexOf('阳光开奖') !== -1 && (txt.indexOf('福彩游戏') !== -1 || txt.indexOf('资料库') !== -1) && txt.length < 300) { el.style.display = 'none'; continue; } if (txt.indexOf('首页') !== -1 && txt.indexOf('游戏') !== -1 && txt.indexOf('公益') !== -1 && txt.indexOf('开奖') !== -1 && txt.length < 120) { el.style.display = 'none'; continue; } } try { var rect = el.getBoundingClientRect(); if (txt === '' && !el.querySelector('img, svg, video, iframe, canvas, input') && rect.top >= 0 && rect.top < 260 && rect.height > 0 && rect.height < 120 && rect.width >= window.innerWidth * 0.7) { el.style.display = 'none'; continue; } var cs = window.getComputedStyle(el); if (cs && cs.position === 'fixed') { if (rect.height > 0 && rect.bottom >= window.innerHeight - 20 && rect.top >= window.innerHeight * 0.5) { el.style.display = 'none'; } } } catch (e) {} } } purifyCwl(); setTimeout(purifyCwl, 600); setTimeout(purifyCwl, 1800); })();
"""

private const val LOTTERY_PURIFY_JS = """
    (function() {
        var els = document.querySelectorAll('header, nav, div, h1, h2, h3, p, span, section');
        for (var i = 0; i < els.length; i++) {
            var el = els[i];
            if (el.querySelector('video, iframe')) continue;
            var txt = (el.innerText || '').trim();
            if (txt.length > 0 && txt.length < 20 && (txt === '相约体彩' || txt === '开奖直播' || txt.indexOf('相约体彩') !== -1)) {
                el.style.display = 'none';
            }
            if (txt.indexOf('开奖直播合作平台') !== -1 && txt.indexOf('往期开奖') === -1 && txt.indexOf('21点25') === -1 && txt.length < 800) { el.style.display = 'none'; }
            if (txt.indexOf('体彩开奖') !== -1 && (txt.indexOf('大乐透') !== -1 || txt.indexOf('排列') !== -1 || txt.indexOf('7星彩') !== -1 || txt.indexOf('开奖号码') !== -1) && txt.indexOf('往期开奖') === -1 && txt.indexOf('21点25') === -1 && txt.length < 1500) { el.style.display = 'none'; }
        }
        if (document.body) {
            document.body.style.marginTop = '0px';
            document.body.style.paddingTop = '4px';
        }
        var containers = document.querySelectorAll('main, .main, .content, .container, .card, #app > div, body > div');
        for (var j = 0; j < containers.length; j++) {
            var c = containers[j];
            if (c.style.display !== 'none') {
                c.style.marginTop = '0px';
                c.style.paddingTop = '4px';
            }
        }
        var videos = document.querySelectorAll('video');
        for (var k = 0; k < videos.length; k++) {
            videos[k].setAttribute('controls', 'controls');
            videos[k].setAttribute('playsinline', 'true');
            videos[k].setAttribute('webkit-playsinline', 'true');
        }
            var lStyleId = 'lottery-purify-style'; if (!document.getElementById(lStyleId)) { var lStyle = document.createElement('style'); lStyle.id = lStyleId; lStyle.textContent = 'html, body { width: 100% !important; max-width: 100% !important; margin: 0 !important; padding: 0 !important; overflow-x: hidden !important; background: #FFFFFF !important; } main, .main, .content, .container, .card, #app > div, body > div { width: 100% !important; max-width: 100% !important; box-sizing: border-box !important; margin: 0 !important; }'; (document.head || document.documentElement).appendChild(lStyle); }
            })();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveScreen(
    sizeClass: ScreenSizeClass,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    initialOrg: LotteryOrg = LotteryOrg.FC
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var activeSource by remember { mutableStateOf(OFFICIAL_SOURCES.first()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(initialOrg) { val targetId = if (initialOrg == LotteryOrg.TC) "tc_live" else "fc_live"; OFFICIAL_SOURCES.find { it.id == targetId }?.let { activeSource = it } }
    var canGoBack by remember { mutableStateOf(false) }

    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val exitVideoCustomView: () -> Unit = {
        customVideoView?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
        }
        customVideoView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        onFullscreenChange(false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.let { act ->
            WindowCompat.getInsetsController(act.window, act.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val handleBackAction: () -> Unit = {
        if (customVideoView != null) {
            exitVideoCustomView()
        } else if (isFullscreen) {
            onFullscreenChange(false)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { act ->
                WindowCompat.getInsetsController(act.window, act.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        }
    }

    BackHandler(enabled = customVideoView != null || isFullscreen || canGoBack) {
        handleBackAction()
    }

    DisposableEffect(Unit) {
        onDispose {
            exitVideoCustomView()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    LaunchedEffect(activeSource.id, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        isLoading = true
        wv.stopLoading()
        wv.loadUrl("about:blank")
        wv.loadUrl(activeSource.url)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 第 1 层：WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = settings.userAgentString + " Mobile"
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false
                            return !targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (url != null && url != "about:blank") {
                                isLoading = true
                                canGoBack = view?.canGoBack() == true
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (url == null || url == "about:blank") return
                            isLoading = false
                            canGoBack = view?.canGoBack() == true
                            if (url.contains("cwl.gov.cn")) {
                                view?.evaluateJavascript(CWL_PURIFY_JS, null)
                            } else if (url.contains("lottery.gov.cn")) {
                                view?.evaluateJavascript(LOTTERY_PURIFY_JS, null)
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view == null) return
                            if (customVideoView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customVideoView = view
                            customViewCallback = callback
                            onFullscreenChange(true)
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            activity?.let { act ->
                                WindowCompat.getInsetsController(act.window, act.window.decorView)
                                    .hide(WindowInsetsCompat.Type.systemBars())
                            }
                        }

                        override fun onHideCustomView() {
                            exitVideoCustomView()
                        }
                    }
                    webViewRef = this
                }
            },
            update = {},
            modifier = Modifier.fillMaxSize()
        )

        // 第 2 层：原生视频全屏视图（渲染在 Box 内，不添加到 decorView）
        customVideoView?.let { videoView ->
            AndroidView(
                factory = {
                    (videoView.parent as? ViewGroup)?.removeView(videoView)
                    videoView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 简短加载提示
        if (isLoading) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xCC161822)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "正在加载${activeSource.label}...",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 第 3 层（永远在最上层）：居中悬浮的 4 项半透明胶囊
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xCC161822)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. 大号「← 返回」按钮
                Surface(
                    onClick = handleBackAction,
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0x33FFFFFF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "返回",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 2 & 3. 「福彩」「体彩」快捷切换按钮
                OFFICIAL_SOURCES.forEach { source ->
                    val selected = activeSource.id == source.id
                    val shortLabel = if (source.id == "fc_live") "福彩" else "体彩"
                    Surface(
                        onClick = {
                            if (customVideoView != null) {
                                exitVideoCustomView()
                            }
                            activeSource = source
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) Color(0xFFD32F2F) else Color(0x26FFFFFF)
                    ) {
                        Text(
                            text = shortLabel,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // 4. 「刷新」按钮
                Surface(
                    onClick = {
                        if (customVideoView != null) {
                            exitVideoCustomView()
                        }
                        isLoading = true
                        webViewRef?.stopLoading()
                        webViewRef?.loadUrl(activeSource.url)
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0x26FFFFFF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "刷新",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        if (activeSource.id == "tc_live" && customVideoView == null) {
            var countdownText by remember { mutableStateOf("") }
            LaunchedEffect(activeSource.id) { val bjTz = TimeZone.getTimeZone("Asia/Shanghai"); while (true) { val now = Calendar.getInstance(bjTz); val nowSec = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND); val startSec = 21 * 3600 + 20 * 60; val endSec = 21 * 3600 + 40 * 60; countdownText = if (nowSec in startSec..endSec) "正在体彩开奖直播中" else { val diff = if (nowSec < startSec) startSec - nowSec else (24 * 3600 - nowSec) + startSec; val hh = (diff / 3600).toString().padStart(2, '0'); val mm = ((diff % 3600) / 60).toString().padStart(2, '0'); val ss = (diff % 60).toString().padStart(2, '0'); "距离直播还有：$hh:$mm:$ss" }; delay(1000L) } }
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), shape = RoundedCornerShape(18.dp), color = Color(0xCC111827)) { Text(text = countdownText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) }
        }
    }
}
