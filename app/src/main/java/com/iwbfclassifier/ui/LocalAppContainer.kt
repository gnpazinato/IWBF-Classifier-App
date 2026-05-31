package com.iwbfclassifier.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.iwbfclassifier.core.di.AppContainer

/** Provides the manual DI container to the Compose tree (set in MainActivity). */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided")
}
