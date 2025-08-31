package com.wokdsem.kioto

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
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
 * @param navigation The NodeNav instance that handles navigation actions.
 */
public fun nodeHost(navigation: NodeNav): UIViewController {
    return ComposeUIViewController {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = with(LocalDensity.current) { maxWidth.toPx() }
            val threshold = width / 2
            val dragEvents = MutableSharedFlow<PredictiveBackEvents>(replay = 1, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            NodeHost(
                bundle = HostBundle(
                    navigation = navigation,
                    platform = Platform.IOS,
                    backHandler = IosPredictiveBackHandler(events = dragEvents)
                )
            )
            Box(modifier = Modifier.fillMaxHeight().width(16.dp).background(Color.Transparent).pointerInput(Unit) {
                var offset = 0f
                detectDragGestures(
                    onDragStart = {
                        offset = 0f
                        dragEvents.tryEmit(value = PredictiveBackEvents.Started)
                    },
                    onDragEnd = { dragEvents.tryEmit(value = if (offset < threshold) PredictiveBackEvents.Cancelled else PredictiveBackEvents.BackPressed) },
                    onDragCancel = { dragEvents.tryEmit(value = PredictiveBackEvents.Cancelled) },
                    onDrag = { _, drag ->
                        offset += drag.x
                        dragEvents.tryEmit(value = PredictiveBackEvents.Progressed(progress = offset.coerceAtLeast(0f) / width))
                    }
                )
            })
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
