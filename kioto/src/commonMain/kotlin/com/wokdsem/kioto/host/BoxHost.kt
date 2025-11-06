package com.wokdsem.kioto.host

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A composable that displays the hosted node at index 0.
 *
 * It provides optional slots for displaying content during loading state or when the hosted nodes list is empty.
 *
 * @param modifier The [Modifier] to be applied to the layout.
 * @param enterTransition The [EnterTransition] to be used when the hosted node is being displayed.
 * @param exitTransition The [ExitTransition] to be used when the hosted node is being hidden.
 * @param loading An optional composable lambda to display while hosted node list is being populated.
 * @param empty An optional composable lambda to display when the hosted node list is empty.
 */
@Composable
public fun BoxHost(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeIn(animationSpec = tween(220)),
    exitTransition: ExitTransition = fadeOut(animationSpec = tween(90)),
    loading: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier) {
        HostedNodesHost { nodes, render ->
            AnimatedContent(
                targetState = nodes,
                transitionSpec = { enterTransition togetherWith exitTransition },
                label = "BoxHostAnimation"
            ) { nodes ->
                when (nodes) {
                    null -> loading?.invoke()
                    0 -> empty?.invoke()
                    else -> render(0)
                }
            }
        }
    }
}
