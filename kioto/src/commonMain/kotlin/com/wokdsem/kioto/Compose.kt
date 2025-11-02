package com.wokdsem.kioto

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition local for the platform hosting NodeNav.
 */
public val LocalPlatform: ProvidableCompositionLocal<Platform?> = staticCompositionLocalOf { null }

public enum class Platform { ANDROID, IOS }

/**
 * Composition local for the scope interface of [NodeScope].
 * This is used to request a back action and determine if the current node is hosted by another node.
 *
 * @see NodeScope
 */
public val LocalNodeScope: ProvidableCompositionLocal<NodeScope?> = staticCompositionLocalOf { null }

public interface NodeScope {
    /**
     * Whether the current node is hosted by another node.
     */
    public val isHosted: Boolean

    /**
     * Requests a back action from the current node.
     */
    public fun navigateBack()
}
