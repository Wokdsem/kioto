package com.wokdsem.kioto.host

import androidx.compose.runtime.Composable
import com.wokdsem.kioto.engine.LocalNodeHostScope

/**
 * A low-level composable that serves as the base component for creating custom implementations
 * for rendering hosted nodes within a [com.wokdsem.kioto.Node].
 *
 * ### Example: Rendering nodes in a vertically distributed list
 * ``` kotlin
 * Column(modifier = Modifier.fillMaxSize()) {
 *   HostedNodesHost { nodes, render ->
 *     repeat(nodes ?: 0) { index ->
 *       Box(modifier = Modifier.fillMaxSize().weight(1F)) {
 *         render(index)
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * @param content A lambda that defines the layout for the hosted nodes. It receives:
 *   - `hostedNodes`: The number of currently hosted nodes, or `null` if current [com.wokdsem.kioto.Node] didn't request to host any nodes.
 *   - `render`: A composable lambda that renders a specific node. You must call this for each
 *     node you want to display, passing its `Int` index (from `0` to `hostedNodes - 1`).
 *     Rendering same index more than once, an out of bounds index, or out of [com.wokdsem.kioto.NodeView] context do nothing.
 */
@Composable
public fun HostedNodesHost(content: @Composable (hostedNodes: Int?, render: @Composable (Int) -> Unit) -> Unit) {
    LocalNodeHostScope.current?.Render(content)
}
