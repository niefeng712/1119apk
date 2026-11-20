package com.example.lotteryinfo.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lotteryinfo.model.LotteryOrg
import com.example.lotteryinfo.ui.layout.ScreenSizeClass
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

data class OfficialLiveSource(
    val id: String,
    val label: String,
    val url: String
)

private val OFFICIAL_SOURCES = listOf(
    OfficialLiveSource("fc_live", "福彩官方直播", "https://www.cwl.gov.cn/html5/ygkj/kjzb/"),
    OfficialLiveSource("tc_live", "体彩官方直播", "https://m.lottery.gov.cn/kjzb/")
)

private val CWL_PURIFY_JS = """
(function() { function isProtected(el) { if (!el || el === document.body || el === document.documentElement) return true; var tag = el.tagName; if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'LINK' || tag === 'VIDEO' || tag === 'IFRAME') return true; if (tag === 'IMG' && (el.getAttribute('src') || '').indexOf('kjzb') !== -1) return true; if (el.querySelector && el.querySelector('video, iframe, img[src*="kjzb"]')) return true; var txt = el.innerText || ''; if (txt.indexOf('距离直播') !== -1 || txt.indexOf('播出渠道') !== -1) return true; return false; } function hideEl(el) { if (!isProtected(el)) { el.style.setProperty('display', 'none', 'important'); } } function purifyCwl() { var blockKw = ['954168', '客服热线', '中国福利彩票官方网站', '阳光开奖', '福彩游戏', '资料库', '双色球：', '快乐8：', '七乐彩：', '福彩3D：', '推荐使用最新版', '360浏览器', '鸿蒙用户', '往期开奖视频', '往期视频', '开奖公告', '往期开奖公告', '开奖流程', '走进开奖现场', '开奖信息发布渠道']; var els = document.querySelectorAll('div, section, header, footer, nav, ul, p, form, table, a, input'); for (var i = 0; i < els.length; i++) { var el = els[i]; if (isProtected(el)) continue; var txt = el.innerText || ''; var matched = false; for (var k = 0; k < blockKw.length; k++) { if (txt.indexOf(blockKw[k]) !== -1) { matched = true; break; } } if (!matched && (el.tagName === 'INPUT' || (el.querySelector && el.querySelector('input')))) matched = true; if (!matched && txt.indexOf('首页') !== -1 && txt.indexOf('游戏') !== -1 && txt.indexOf('公益') !== -1 && txt.indexOf('开奖') !== -1) matched = true; var st = window.getComputedStyle(el); var rect = el.getBoundingClientRect(); if (!matched && st.position === 'fixed' && rect.bottom >= window.innerHeight - 60 && rect.height > 0 && rect.height < 160) matched = true; if (!matched && rect.top >= -10 && rect.top < 200 && rect.height >= 2 && rect.height <= 80 && rect.width > window.innerWidth * 0.7 && txt.trim() === '' && !el.querySelector('img, video, iframe, svg, canvas')) matched = true; if (!isProtected(el) && txt.indexOf('福彩开奖') !== -1 && txt.indexOf('双色球') !== -1) { matched = true; if (el.nextElementSibling && !isProtected(el.nextElementSibling)) hideEl(el.nextElementSibling); } if (!matched && !isProtected(el) && txt.indexOf('开奖视频') !== -1) matched = true; if (matched) hideEl(el); } } purifyCwl();
setTimeout(purifyCwl, 600);
  setTimeout(purifyCwl, 1800);
})();
""".trimIndent()

private val LOTTERY_PURIFY_JS = """
(function() {
  var STYLE_ID = 'tc-purify-style';
  if (!document.getElementById(STYLE_ID)) {
    var s = document.createElement('style');
    s.id = STYLE_ID;
    s.textContent = 'html, body { background: #111315 !important; overflow-x: hidden !important; margin: 0 !important; padding: 0 !important; }';
    (document.head || document.documentElement).appendChild(s);
  }

  function isExcludedBlock(t) {
    return t.indexOf('合作平台') !== -1 || t.indexOf('体彩开奖') !== -1 || t.indexOf('往期') !== -1;
  }

  function findTcLiveCard() {
    var media = document.querySelector('video, iframe');
    if (media) {
      var cur = media;
      while (cur && cur.parentElement && cur.parentElement !== document.body) {
        var r = cur.parentElement.getBoundingClientRect();
        var txt = cur.parentElement.innerText || '';
        if (isExcludedBlock(txt) || /\b\d{2}-\d{2}\b/.test(txt) || r.height > 520) {
          return cur;
        }
        cur = cur.parentElement;
      }
      return cur;
    }
    var all = document.querySelectorAll('div, section');
    var best = null;
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      var t = el.innerText || '';
      if (t.indexOf('每晚21点25') !== -1 || t.indexOf('敬请期待') !== -1 || t.indexOf('开奖直播') !== -1) {
        if (!isExcludedBlock(t) && !/\b\d{2}-\d{2}\b/.test(t) && t.indexOf('相约体彩') === -1) {
          var rect = el.getBoundingClientRect();
          if (rect.height >= 120 && rect.height <= 500 && rect.width >= 150) {
            best = el;
          }
        }
      }
    }
    return best;
  }

  function purifyTc() {
    document.documentElement.style.setProperty('background', '#111315', 'important');
    document.documentElement.style.setProperty('overflow-x', 'hidden', 'important');
    if (document.body) {
      document.body.style.setProperty('background', '#111315', 'important');
      document.body.style.setProperty('overflow-x', 'hidden', 'important');
      document.body.style.setProperty('margin', '0', 'important');
      document.body.style.setProperty('padding', '0', 'important');
    }

    var card = findTcLiveCard();
    if (!card) return;

    var cur = card;
    while (cur && cur !== document.body && cur.parentElement) {
      var parent = cur.parentElement;
      var children = parent.children;
      for (var i = 0; i < children.length; i++) {
        var sib = children[i];
        if (sib !== cur && sib.tagName !== 'SCRIPT' && sib.tagName !== 'STYLE' && sib.tagName !== 'LINK') {
          sib.style.setProperty('display', 'none', 'important');
        }
      }
      if (parent !== document.body) {
        parent.style.setProperty('margin', '0 auto', 'important');
        parent.style.setProperty('padding', '0', 'important');
        parent.style.setProperty('background', 'transparent', 'important');
        parent.style.setProperty('display', 'flex', 'important');
        parent.style.setProperty('flex-direction', 'column', 'important');
        parent.style.setProperty('align-items', 'center', 'important');
      }
      cur = parent;
    }

    var allEls = document.querySelectorAll('div, section, header, footer, ul, nav');
    for (var j = 0; j < allEls.length; j++) {
      var el = allEls[j];
      if (el === card || card.contains(el) || el.contains(card)) continue;
      var txt = el.innerText || '';
      if (
        txt.indexOf('相约体彩') !== -1 ||
        txt.indexOf('开奖直播合作平台') !== -1 ||
        txt.indexOf('体彩开奖') !== -1 ||
        txt.indexOf('往期') !== -1 ||
        /\b\d{2}-\d{2}\b/.test(txt)
      ) {
        el.style.setProperty('display', 'none', 'important');
      }
    }
  }

  purifyTc();
  setTimeout(purifyTc, 600);
  setTimeout(purifyTc, 1800);
})();
""".trimIndent()

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
    val configuration = LocalConfiguration.current

    var activeSource by remember {
        mutableStateOf(
            if (initialOrg == LotteryOrg.TC) OFFICIAL_SOURCES[1] else OFFICIAL_SOURCES[0]
        )
    }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var countdownText by remember { mutableStateOf("") }

    LaunchedEffect(initialOrg) {
        val targetId = if (initialOrg == LotteryOrg.TC) "tc_live" else "fc_live"
        OFFICIAL_SOURCES.find { it.id == targetId }?.let { target ->
            if (activeSource.id != target.id) {
                activeSource = target
                isLoading = true
                webViewRef?.stopLoading()
                webViewRef?.loadUrl(target.url)
            }
        }
    }

    LaunchedEffect(activeSource.id) {
        if (activeSource.id != "tc_live") {
            countdownText = ""
            return@LaunchedEffect
        }
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        while (true) {
            val now = Calendar.getInstance(tz)
            val h = now.get(Calendar.HOUR_OF_DAY)
            val m = now.get(Calendar.MINUTE)
            val s = now.get(Calendar.SECOND)
            val secOfDay = h * 3600 + m * 60 + s
            val startSec = 21 * 3600 + 20 * 60
            val endSec = 21 * 3600 + 40 * 60
            countdownText = if (secOfDay in startSec until endSec) {
                "正在体彩开奖直播中"
            } else {
                val diff = if (secOfDay < startSec) {
                    startSec - secOfDay
                } else {
                    24 * 3600 - secOfDay + startSec
                }
                val dh = diff / 3600
                val dm = (diff % 3600) / 60
                val ds = diff % 60
                "距离体彩直播还有：%02d:%02d:%02d".format(dh, dm, ds)
            }
            delay(1000L)
        }
    }

    val exitVideoCustomView: () -> Unit = {
        customViewCallback?.onCustomViewHidden()
        customVideoView = null
        customViewCallback = null
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    val handleBackAction: () -> Unit = {
        when {
            customVideoView != null -> exitVideoCustomView()
            isFullscreen -> {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onFullscreenChange(false)
            }
            else -> onFullscreenChange(false)
        }
    }

    BackHandler(enabled = customVideoView != null || isFullscreen) {
        handleBackAction()
    }

    LaunchedEffect(configuration.orientation, customVideoView) {
        if (customVideoView != null) {
            onFullscreenChange(true)
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onFullscreenChange(true)
        } else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            onFullscreenChange(false)
        }
    }

    DisposableEffect(isFullscreen, customVideoView) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (isFullscreen || customVideoView != null) {
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (customVideoView == null) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    val switchSource: (OfficialLiveSource) -> Unit = { source ->
        if (activeSource.id != source.id) {
            if (customVideoView != null) {
                exitVideoCustomView()
            }
            activeSource = source
            isLoading = true
            webViewRef?.stopLoading()
            webViewRef?.loadUrl(source.url)
        }
    }

    val refreshSource: () -> Unit = {
        if (customVideoView != null) {
            exitVideoCustomView()
        }
        isLoading = true
        webViewRef?.stopLoading()
        webViewRef?.loadUrl(activeSource.url)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111315))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111315))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                LiveTopCapsule(
                    activeSource = activeSource,
                    onBack = handleBackAction,
                    onSelectSource = switchSource,
                    onRefresh = refreshSource
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.parseColor("#111315"))
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString =
                                    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean = false

                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    val js = if (url?.contains("lottery.gov.cn") == true) {
                                        LOTTERY_PURIFY_JS
                                    } else {
                                        CWL_PURIFY_JS
                                    }
                                    view?.evaluateJavascript(js, null)
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(
                                    view: View?,
                                    callback: CustomViewCallback?
                                ) {
                                    if (view == null) return
                                    if (customVideoView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    customVideoView = view
                                    customViewCallback = callback
                                    onFullscreenChange(true)
                                }

                                override fun onHideCustomView() {
                                    customVideoView = null
                                    customViewCallback?.onCustomViewHidden()
                                    customViewCallback = null
                                }
                            }
                            webViewRef = this
                            loadUrl(activeSource.url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF111315)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE53935))
                    }
                }
            }

            if (activeSource.id == "tc_live" && countdownText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdownText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (customVideoView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            (customVideoView?.parent as? ViewGroup)?.removeView(customVideoView)
                            addView(
                                customVideoView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiveTopCapsule(
                        activeSource = activeSource,
                        onBack = handleBackAction,
                        onSelectSource = switchSource,
                        onRefresh = refreshSource
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTopCapsule(
    activeSource: OfficialLiveSource,
    onBack: () -> Unit,
    onSelectSource: (OfficialLiveSource) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xCC1E2228),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(18.dp),
                color = Color(0x26FFFFFF)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "返回",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                onClick = { onSelectSource(OFFICIAL_SOURCES[0]) },
                shape = RoundedCornerShape(18.dp),
                color = if (activeSource.id == "fc_live") Color(0xFFE53935) else Color.Transparent
            ) {
                Text(
                    text = "福彩",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (activeSource.id == "fc_live") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Surface(
                onClick = { onSelectSource(OFFICIAL_SOURCES[1]) },
                shape = RoundedCornerShape(18.dp),
                color = if (activeSource.id == "tc_live") Color(0xFF1E88E5) else Color.Transparent
            ) {
                Text(
                    text = "体彩",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (activeSource.id == "tc_live") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Surface(
                onClick = onRefresh,
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
}

