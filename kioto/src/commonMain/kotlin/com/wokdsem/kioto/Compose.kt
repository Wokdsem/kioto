package com.wokdsem.kioto

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition local for the NodeHost's context.
 */
public val LocalNodeHost: ProvidableCompositionLocal<NodeHost?> = staticCompositionLocalOf { null }

public interface NodeHost {

    /**
     * Whether a navigation transition between [Node] is currently in progress.
     */
    public val isTransitionInProgress: Boolean

    /**
     * The system host platform.
     * @see Platform
     */
    public val platform: Platform

}

/**
 * Supported host platforms.
 */
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
