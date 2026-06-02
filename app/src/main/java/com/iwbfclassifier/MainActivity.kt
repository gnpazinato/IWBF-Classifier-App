package com.iwbfclassifier

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.navigation.AppNavHost
import com.iwbfclassifier.ui.theme.ClassifierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Field-ready behaviour (user request):
        //  - never auto-lock / dim while observing,
        //  - run truly full screen, hiding the status bar (battery/clock) and the
        //    bottom navigation bar so nothing overlaps the app chrome.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val container = (application as ClassifierApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                ClassifierTheme {
                    AppNavHost()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-assert immersive mode whenever we regain focus (after dialogs, the
        // keyboard, or a transient swipe-down of the system bars).
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
