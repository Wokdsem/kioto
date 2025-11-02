package com.wokdsem.kioto.host

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A composable that displays a collection of hosted nodes one at a time, using a pager-like interface.
 *
 * It provides optional slots for displaying content during loading state or when the data set is empty.
 *
 * @param state The state object to be used for controlling and observing the pager's state.
 * @param modifier The [Modifier] to be applied to this pager host layout.
 * @param loading An optional composable lambda to display while hosted node list is being populated.
 * @param empty An optional composable lambda to display when the hosted node list is empty.
 */
@Composable
public fun PagerHost(
    state: PagerHostState,
    modifier: Modifier = Modifier,
    loading: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier) {
        HostedNodesHost { nodes, render ->
            LaunchedEffect(nodes) { state.pageCount = nodes }
            val targetState: PagerState = when (nodes) {
                null -> PagerState.Loading
                0 -> PagerState.Empty
                else -> PagerState.Content(state.currentPage.coerceIn(0, nodes - 1))
            }
            AnimatedContent(
                targetState = targetState,
                label = "PagerHostAnimation"
            ) { pagerState ->
                when (pagerState) {
                    PagerState.Loading -> loading?.invoke()
                    PagerState.Empty -> empty?.invoke()
                    is PagerState.Content -> key(pagerState.page) {
                        render(pagerState.page)
                    }
                }
            }
        }
    }
}

/**
 * A state object to control and observe the pager host's state.
 *
 * @see rememberPagerHostState
 */
@Stable
public class PagerHostState internal constructor(
    initialPage: Int = 0
) {
    public var currentPage: Int by mutableStateOf(initialPage)
    public var pageCount: Int? by mutableStateOf(null)
        internal set
}

/**
 * Creates and remembers a [PagerHostState] instance.
 *
 * @param currentPage The initial page to display.
 */
@Composable
public fun rememberPagerHostState(
    currentPage: Int = 0
): PagerHostState = rememberSaveable(saver = listSaver(save = { listOf(it.currentPage) }, restore = { PagerHostState(it[0]) })) {
    PagerHostState(currentPage)
}

private sealed interface PagerState {
    data object Loading : PagerState
    data object Empty : PagerState
    data class Content(val page: Int) : PagerState
}