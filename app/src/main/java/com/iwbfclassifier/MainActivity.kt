package com.iwbfclassifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.iwbfclassifier.ui.LocalAppContainer
import com.iwbfclassifier.ui.navigation.AppNavHost
import com.iwbfclassifier.ui.theme.ClassifierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ClassifierApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                ClassifierTheme {
                    AppNavHost()
                }
            }
        }
    }
}
