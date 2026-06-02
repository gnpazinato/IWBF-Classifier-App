package com.iwbfclassifier.ui.components

import android.annotation.SuppressLint
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
 * workflow (docs/06): [currentSeconds] reads the player's live position so "Flag
 * Moment" is one tap, and [playWindow] replays a slow-motion window in-app.
 *
 * All JS calls are marshalled onto the WebView's UI thread; reads are served from a
 * value the page pushes back ~4x/second, so they are cheap and synchronous.
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

    /** Cue a video (no autoplay). Safe to call before the player is ready — it queues. */
    fun load(videoId: String?, startSeconds: Int = 0) {
        if (videoId.isNullOrBlank()) return
        if (isReady) {
            loadedVideoId = videoId
            js("player.cueVideoById({videoId:'$videoId', startSeconds:$startSeconds});")
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
            js("player.cueVideoById({videoId:'$v', startSeconds:$pendingStart});")
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

private class YouTubeJsBridge(private val controller: YouTubePlayerController) {
    @JavascriptInterface
    fun onReady() = controller.onReadyInternal()

    @JavascriptInterface
    fun onTime(seconds: Double) = controller.onTimeInternal(seconds)
}

/**
 * Embedded YouTube player surface. Renders a WebView hosting the official IFrame
 * Player API — no video is downloaded (docs/06), only streamed/embedded. Drive it
 * through [controller].
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
                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                addJavascriptInterface(YouTubeJsBridge(controller), "Android")
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
<style>html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}#player{width:100%;height:100%}</style>
</head>
<body>
<div id="player"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var player = null;
var endTime = -1;
function onYouTubeIframeAPIReady() {
  player = new YT.Player('player', {
    width: '100%', height: '100%',
    playerVars: { playsinline: 1, rel: 0, modestbranding: 1, controls: 1, fs: 1 },
    events: {
      'onReady': function() { if (window.Android && Android.onReady) Android.onReady(); }
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
