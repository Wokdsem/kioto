package com.wokdsem.kioto

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition local for the platform hosting NodeNav.
 */
public val LocalPlatform: ProvidableCompositionLocal<Platform?> = staticCompositionLocalOf { null }

public enum class Platform { ANDROID, IOS }

/**
 * Composition local for the navigation interface of [NodeNavigation].
 * This is used to request a back action.
 *
 * @see NodeNavigation
 */
public val LocalNodeNavigation: ProvidableCompositionLocal<NodeNavigation?> = staticCompositionLocalOf { null }

public fun interface NodeNavigation {
    /**
     * Requests a back action from the current node.
     */
    public fun navigateBack()
}

internal val LocalBackHandler: ProvidableCompositionLocal<BackHandler?> = staticCompositionLocalOf { null }
