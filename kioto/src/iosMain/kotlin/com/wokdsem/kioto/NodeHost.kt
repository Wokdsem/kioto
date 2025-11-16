package com.wokdsem.kioto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackEventCompat
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.window.ComposeUIViewController
import com.wokdsem.kioto.engine.BackHandler
import com.wokdsem.kioto.engine.HostBundle
import com.wokdsem.kioto.engine.NodeHost
import com.wokdsem.kioto.engine.NodeNavRunner
import com.wokdsem.kioto.engine.Releasable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import platform.UIKit.UIViewController

/**
 * Creates a [UIViewController] that renders the state of a [NodeNav].
 *
 * @see NodeNav
 *
 * @param nodeNav The NodeNav instance that handles navigation actions.
 * @param wrapper A composable function that wraps the NodeHost. It receives a `nodeHost` lambda parameter which *must* be called to render the actual navigation content.
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun nodeHost(
    nodeNav: NodeNav,
    wrapper: @Composable (nodeHost: @Composable () -> Unit) -> Unit = { nodeHost -> nodeHost() }
): UIViewController {
    val backEvents = MutableSharedFlow<PredictiveBackEvents>(replay = 1, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val bundle = HostBundle(
        nodeNav = nodeNav as NodeNavRunner,
        platform = Platform.IOS,
        backHandler = IosPredictiveBackHandler(events = backEvents)
    )
    return ComposeUIViewController {
        PredictiveBackHandler { progress: Flow<BackEventCompat> ->
            try {
                backEvents.tryEmit(value = PredictiveBackEvents.Started)
                progress.collect { backEvent -> backEvents.tryEmit(value = PredictiveBackEvents.Progressed(progress = backEvent.progress)) }
                backEvents.tryEmit(value = PredictiveBackEvents.BackPressed)
            } catch (e: Exception) {
                backEvents.tryEmit(value = PredictiveBackEvents.Cancelled)
                throw e
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            wrapper {
                NodeHost(bundle = bundle)
            }
        }
    }
}

private class IosPredictiveBackHandler(
    private val events: Flow<PredictiveBackEvents>
) : BackHandler {
    override fun register(callback: BackHandler.Callback): Releasable {
        return events.dropWhile { it is PredictiveBackEvents.BackPressed || it is PredictiveBackEvents.Cancelled }.run {
            var firstRun = false
            onEach { event ->
                if (!firstRun) {
                    if (event is PredictiveBackEvents.Progressed) callback.onBackStarted()
                    firstRun = true
                }
                when (event) {
                    PredictiveBackEvents.Started -> callback.onBackStarted()
                    is PredictiveBackEvents.Progressed -> callback.onBackProgressed(event.progress)
                    PredictiveBackEvents.Cancelled -> callback.onBackCancelled()
                    PredictiveBackEvents.BackPressed -> callback.onBackPressed()
                }
            }
        }.launchIn(CoroutineScope(Dispatchers.Main)).let { Releasable(it::cancel) }
    }
}

private sealed interface PredictiveBackEvents {
    data object Started : PredictiveBackEvents
    class Progressed(val progress: Float) : PredictiveBackEvents
    data object Cancelled : PredictiveBackEvents
    data object BackPressed : PredictiveBackEvents
}
