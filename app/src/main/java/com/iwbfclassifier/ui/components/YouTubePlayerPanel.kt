package com.iwbfclassifier.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Controls an embedded YouTube IFrame player. The key capability for the classifier
 * workflow (docs/06): [currentSeconds] reads the player's live position so "Add Moment"
 * is one tap, and [playWindow] replays a slow-motion window in-app.
 */
class YouTubePlayerController {
    internal var webView: WebView? = null

    @Volatile
    var isReady: Boolean = false
        private set

    @Volatile
    private var lastSeconds: Double = 0.0

    var loadedVideoId: String? = null
        private set

    private var pendingVideoId: String? = null
    private var pendingStart: Int = 0

    /** Latest known player position in seconds (0 until the page reports one). */
    fun currentSeconds(): Double = lastSeconds

    /** Load + play a video. Safe to call before the player is ready — it queues. */
    fun load(videoId: String?, startSeconds: Int = 0) {
        if (videoId.isNullOrBlank()) return
        if (isReady) {
            loadedVideoId = videoId
            js("loadVid('$videoId', $startSeconds);")
        } else {
            pendingVideoId = videoId
            pendingStart = startSeconds
        }
    }

    /** Seek to [startSeconds], set [rate], play, and auto-pause at [endSeconds] (in-app slow-mo replay). */
    fun playWindow(startSeconds: Int, endSeconds: Int?, rate: Double) {
        js("playWindow($startSeconds, ${endSeconds ?: -1}, $rate);")
    }

    fun play() = js("if(player&&player.playVideo)player.playVideo();")
    fun pause() = js("if(player&&player.pauseVideo)player.pauseVideo();")

    internal fun onReadyInternal() {
        isReady = true
        pendingVideoId?.let { v ->
            loadedVideoId = v
            js("loadVid('$v', $pendingStart);")
            pendingVideoId = null
        }
    }

    internal fun onTimeInternal(t: Double) {
        lastSeconds = t
    }

    private fun js(script: String) {
        val wv = webView ?: return
        wv.post { wv.evaluateJavascript(script, null) }
    }
}

private class YouTubeJsBridge(
    private val controller: YouTubePlayerController,
    private val appContext: Context,
) {
    @JavascriptInterface
    fun onReady() = controller.onReadyInternal()

    @JavascriptInterface
    fun onTime(seconds: Double) = controller.onTimeInternal(seconds)

    /** Open the current video in the YouTube app / browser (fallback when embed is blocked). */
    @JavascriptInterface
    fun openInYouTube() {
        val id = controller.loadedVideoId ?: return
        val url = "https://www.youtube.com/watch?v=$id"
        controller.webView?.post {
            runCatching {
                appContext.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

/**
 * Embedded YouTube player surface. Renders a WebView hosting the official IFrame
 * Player API — no video is downloaded (docs/06), only streamed/embedded. The page
 * forces the player to fill the whole surface (no inner scrolling) and shows a
 * "this video can't be embedded → Open in YouTube" fallback on embed errors.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerPanel(
    controller: YouTubePlayerController,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                addJavascriptInterface(YouTubeJsBridge(controller, ctx.applicationContext), "Android")
                controller.webView = this
                loadDataWithBaseURL("https://www.youtube.com", PLAYER_HTML, "text/html", "utf-8", null)
            }
        },
        onRelease = { wv ->
            controller.webView = null
            wv.destroy()
        },
    )
}

private const val PLAYER_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<!-- YouTube (since mid-2025) rejects embeds that don't send a Referer identifying the
     host, returning error 152/153 even for embeddable videos. Forcing the origin referrer
     here makes the WebView send it (loadDataWithBaseURL otherwise often sends none). -->
<meta name="referrer" content="origin">
<style>
  html,body{margin:0;padding:0;background:#000;height:100%;width:100%;overflow:hidden}
  body{position:relative}
  #player,iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}
  #err{display:none;position:absolute;top:0;left:0;right:0;bottom:0;background:#0E0E0E;color:#fff;
       flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:16px;
       font-family:sans-serif}
  #err p{margin:0 0 14px;font-size:15px;color:#D7D7D7}
  #err button{background:#A3975D;color:#0E0E0E;border:0;border-radius:10px;padding:12px 20px;
       font-size:15px;font-weight:bold}
</style>
</head>
<body>
<div id="player"></div>
<div id="err" onclick="hideErr()"><p id="errmsg">This video can't be played here.</p>
  <button onclick="event.stopPropagation(); Android.openInYouTube()">Open in YouTube</button>
  <p style="font-size:12px;color:#888;margin-top:12px">Tap to dismiss</p></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var player = null;
var endTime = -1;
function showErr(msg){ document.getElementById('errmsg').textContent = msg;
  document.getElementById('err').style.display='flex'; }
function hideErr(){ document.getElementById('err').style.display='none'; }
function onYouTubeIframeAPIReady() {
  player = new YT.Player('player', {
    width: '100%', height: '100%',
    // Privacy-enhanced host — more permissive for embeds and recommended for the 2025
    // referer change; the embedding page origin stays youtube.com (see playerVars).
    host: 'https://www.youtube-nocookie.com',
    // enablejsapi + origin are REQUIRED for the IFrame API to work inside an Android
    // WebView: origin must match the WebView base URL (https://www.youtube.com), otherwise
    // YouTube refuses the embed and reports error 150/153 even for embeddable videos.
    playerVars: { playsinline: 1, rel: 0, modestbranding: 1, controls: 1, fs: 1, autoplay: 1,
                  enablejsapi: 1, origin: 'https://www.youtube.com' },
    events: {
      'onReady': function() { if (window.Android && Android.onReady) Android.onReady(); },
      'onStateChange': function(e) { if (e.data == 1 || e.data == 3) hideErr(); },
      'onError': function(e) {
        // Show the exact code so a failing video can be diagnosed precisely. The overlay
        // auto-hides if playback then starts (onStateChange), so a transient error is fine.
        var c = e.data;
        var msg;
        if (c == 101 || c == 150) msg = "The video owner blocked playback in embedded players.";
        else if (c == 100) msg = "This video is unavailable (removed or private).";
        else if (c == 5) msg = "This video can't be played in the in-app player.";
        else if (c == 2) msg = "Invalid video link.";
        else msg = "This video can't be played here.";
        showErr(msg + " (error " + c + ")");
      }
    }
  });
  setInterval(function() {
    try {
      if (player && player.getCurrentTime) {
        var t = player.getCurrentTime();
        if (window.Android && Android.onTime) Android.onTime(t);
        if (endTime > 0 && t >= endTime) { player.pauseVideo(); endTime = -1; }
      }
    } catch (e) {}
  }, 250);
}
function loadVid(id, start) { if (!player) return; hideErr(); endTime = -1; player.loadVideoById({videoId: id, startSeconds: start}); }
function playWindow(start, end, rate) {
  if (!player) return;
  endTime = end;
  try { player.seekTo(start, true); } catch (e) {}
  try { player.setPlaybackRate(rate); } catch (e) {}
  try { player.playVideo(); } catch (e) {}
}
</script>
</body>
</html>
"""
