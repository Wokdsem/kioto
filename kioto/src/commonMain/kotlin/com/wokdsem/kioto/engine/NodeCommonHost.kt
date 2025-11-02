package com.wokdsem.kioto.engine

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import com.wokdsem.kioto.LocalNodeScope
import com.wokdsem.kioto.LocalPlatform
import com.wokdsem.kioto.NodeScope
import com.wokdsem.kioto.Platform
import com.wokdsem.kioto.engine.Animation.Type
import com.wokdsem.kioto.engine.NodeNavRunner.Pane
import com.wokdsem.kioto.engine.NodeNavRunner.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi

private const val ANIM_DURATION_SLIDE = 340
private const val ANIM_DURATION_FADE = 280
private const val FACTOR_OVERLAY = 0.16F
private const val FACTOR_TRANSLATION = 0.3F

internal val LocalNodeHostScope = staticCompositionLocalOf<NodeHostScope?> { null }

internal data class HostBundle(
    val nodeNav: NodeNavRunner,
    val platform: Platform,
    val backHandler: BackHandler
)

internal fun interface BackHandler {
    fun register(callback: Callback): Releasable

    interface Callback {
        fun onBackStarted()
        fun onBackProgressed(progress: Float)
        fun onBackPressed()
        fun onBackCancelled()
    }
}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalComposeUiApi::class, ExperimentalUuidApi::class)
@Composable
internal fun NodeHost(bundle: HostBundle) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val hostStateHolder = rememberSaveableStateHolder()
        val heldNodes = rememberSaveable { mutableListOf<String>() }
        val visiblePanes = remember { mutableStateOf<List<ActivePane>>(emptyList()) }
        LaunchedEffect(key1 = bundle, key2 = maxWidth) { runHost(visiblePanes, heldNodes, hostStateHolder, bundle, maxWidth) }
        CompositionLocalProvider(LocalPlatform provides bundle.platform) {
            Layout(
                content = {
                    @Composable
                    fun <S : Any> Render(stateHolder: SaveableStateHolder, pane: Pane<S>, hosted: Boolean) {
                        class HostedPanes(val panes: Array<Pane<*>>? = null)

                        val nodeScope = remember {
                            object : NodeScope {
                                override val isHosted: Boolean get() = hosted
                                override fun navigateBack() = pane.navBack()
                            }
                        }
                        val nodeHostScope = remember {
                            NodeHostScope { content ->
                                val hostedHostStateHolder = rememberSaveableStateHolder()
                                val hostedHeldNodes = rememberSaveable { mutableListOf<String>() }
                                var hostedPanes by remember { mutableStateOf<HostedPanes?>(null) }
                                LaunchedEffect(pane.hostedPanes) {
                                    pane.hostedPanes.onEach { hostedNodes ->
                                        while (hostedHeldNodes.isNotEmpty() && hostedHeldNodes.last() != hostedNodes?.lastOrNull()?.id) {
                                            hostedHeldNodes.removeLast().let(hostedHostStateHolder::removeState)
                                        }
                                        hostedHeldNodes += hostedNodes?.map { it.id } ?: emptyList()
                                    }.collect { hostedPanes = HostedPanes(it) }
                                }
                                if (hostedPanes != null) {
                                    val panes = hostedPanes?.panes
                                    val renderedNodes = mutableSetOf<Int>()
                                    content(panes?.size) { paneIndex ->
                                        if (panes == null || paneIndex < 0 || paneIndex > panes.lastIndex || paneIndex in renderedNodes) return@content
                                        renderedNodes += paneIndex
                                        key(paneIndex) {
                                            Render(stateHolder = hostedHostStateHolder, pane = panes[paneIndex], hosted = true)
                                            DisposableEffect(Unit) {
                                                onDispose { renderedNodes -= paneIndex }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        CompositionLocalProvider(
                            LocalNodeScope provides nodeScope,
                            LocalNodeHostScope provides nodeHostScope
                        ) {
                            stateHolder.SaveableStateProvider(pane.id) {
                                val state by pane.state.collectAsState()
                                pane.content(state)
                            }
                        }
                    }
                    visiblePanes.value.forEach { (visiblePane, modifier) ->
                        key(visiblePane.id) {
                            Box(modifier) {
                                Render(stateHolder = hostStateHolder, pane = visiblePane.pane, hosted = false)
                            }
                        }
                    }
                },
                measurePolicy = { panes, constraints ->
                    layout(width = constraints.maxWidth, height = constraints.maxHeight) {
                        panes.forEach { it.measure(constraints).place(0, 0, 0f) }
                    }
                }
            )
        }
    }
}

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun runHost(
    visiblePanes: MutableState<List<ActivePane>>,
    heldNodes: MutableList<String>,
    saveableStateHolder: SaveableStateHolder,
    bundle: HostBundle,
    maxWidth: Dp
) {
    var visiblePanes by visiblePanes
    var firstActivePanesHandled = false
    var pastTransitionProgress = 1F
    val (nodeNav, platform, backHandler) = bundle
    nodeNav.activePanes.onEach { activePanes ->
        if (heldNodes.isNotEmpty() && heldNodes.last() == activePanes.activeIds.lastOrNull()?.id) return@onEach
        val refId = when (activePanes.transition) {
            Transition.BEGIN_STACK, Transition.PRESENT_STACK, Transition.SIBLING, Transition.REPLACE -> activePanes.activeIds.getOrNull(activePanes.activeIds.lastIndex - 1)
            Transition.CLOSE_STACK, Transition.CLOSE_PRESENTED_STACK, Transition.BACK -> activePanes.activeIds.lastOrNull()
        }?.id
        while (heldNodes.isNotEmpty() && heldNodes.last() != refId) heldNodes.removeAt(heldNodes.lastIndex).let(saveableStateHolder::removeState)
        if (activePanes.transition.run { this == Transition.BEGIN_STACK || this == Transition.PRESENT_STACK || this == Transition.SIBLING || this == Transition.REPLACE }) {
            if (heldNodes.isEmpty() && activePanes.activeIds.size == 2) heldNodes += activePanes.activeIds.first().id
            heldNodes += activePanes.activeIds.last().id
        }
    }.mapLatest { activePanes ->
        val target = StablePane(pane = activePanes.foreground)
        val backPressedToken = backHandler.takeUnless { platform == Platform.IOS || activePanes.background == null }
            ?.register(callback = BackPressedCallback(activePanes.foreground.navBack))
        try {
            if (firstActivePanesHandled) {
                withContext(Dispatchers.IO) {
                    val noticeableBackward = visiblePanes.size == 2 && visiblePanes.first().id == target.id
                    val (animator, duration, type, targetModifier, currentModifier) = when {
                        noticeableBackward -> backwardAnimation(maxWidth, pastTransitionProgress)
                        visiblePanes.size != 1 -> replaceAnimation()
                        else -> when (activePanes.transition) {
                            Transition.BEGIN_STACK, Transition.SIBLING -> forwardAnimation(maxWidth)
                            Transition.CLOSE_STACK, Transition.BACK -> backwardAnimation(maxWidth)
                            Transition.PRESENT_STACK, Transition.REPLACE, Transition.CLOSE_PRESENTED_STACK -> replaceAnimation()
                        }
                    }
                    visiblePanes = when (type) {
                        Type.FORWARD -> visiblePanes.map { it.copy(modifier = it.modifier then currentModifier) } + ActivePane(target, targetModifier)

                        Type.REPLACE -> (visiblePanes.asSequence().filter { it.id != target.id }.map { it.copy(modifier = it.modifier then currentModifier) } +
                                ActivePane(target, targetModifier)).toList()

                        Type.BACKWARD -> when {
                            noticeableBackward -> listOf(visiblePanes[0].copy(modifier = targetModifier), visiblePanes[1].copy(modifier = currentModifier))
                            else -> {
                                when (val targetIndex = visiblePanes.indexOfFirst { it.id == target.id }) {
                                    -1 -> listOf(ActivePane(target, targetModifier)) + visiblePanes.map { it.copy(modifier = it.modifier then currentModifier) }
                                    else -> visiblePanes.mapIndexed { index, pane -> pane.copy(modifier = if (targetIndex == index) targetModifier else pane.modifier then currentModifier) }
                                }
                            }
                        }
                    }
                    runCatching {
                        animator.animateTo(1F, animationSpec = tween(durationMillis = duration, easing = LinearEasing))
                    }.onFailure { pastTransitionProgress = animator.value }.getOrThrow()
                }
            }
        } catch (e: Throwable) {
            backPressedToken?.release()
            throw e
        }

        val targetPanes = listOf(ActivePane(pane = target, modifier = Modifier))
        firstActivePanesHandled = true
        visiblePanes = targetPanes

        if (activePanes.background !is NodeNavRunner.ActivePanes.BackgroundPane) pastTransitionProgress = 1F
        if (activePanes.background is NodeNavRunner.ActivePanes.HandledBackground) return@mapLatest backPressedToken?.awaitBeforeRelease()

        backPressedToken?.release()
        if (activePanes.background is NodeNavRunner.ActivePanes.BackgroundPane) {
            val predictiveEvents: Channel<suspend () -> Unit> = Channel(capacity = Channel.BUFFERED, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            val backPredictiveAnimator = Animatable(0F)
            val predictiveBackToken = backHandler.register(object : BackHandler.Callback {
                val predictiveModifiers = when (activePanes.background.presented) {
                    true -> fadeIn(backPredictiveAnimator.asState()) to fadeOut(backPredictiveAnimator.asState())
                    else -> slideInFromLeft(maxWidth, backPredictiveAnimator.asState()) to slideOutToRight(maxWidth, backPredictiveAnimator.asState())
                }
                val predictivePanes = listOf(
                    ActivePane(pane = StablePane(activePanes.background.backgroundPane), modifier = predictiveModifiers.first),
                    ActivePane(pane = target, modifier = predictiveModifiers.second)
                )

                override fun onBackStarted() {
                    predictiveEvents.trySend {
                        visiblePanes = predictivePanes
                        backPredictiveAnimator.animateTo(0F)
                    }
                }

                override fun onBackProgressed(progress: Float) {
                    predictiveEvents.trySend {
                        backPredictiveAnimator.snapTo(progress)
                    }
                }

                override fun onBackPressed() {
                    target.pane.navBack()
                }

                override fun onBackCancelled() {
                    predictiveEvents.trySend {
                        backPredictiveAnimator.animateTo(0F)
                        visiblePanes = targetPanes
                    }
                }
            })
            try {
                while (true) predictiveEvents.receive().invoke()
            } catch (e: Throwable) {
                pastTransitionProgress = 1 - backPredictiveAnimator.value
                throw e
            } finally {
                predictiveBackToken.release()
            }
        }
    }.collect()
}

private suspend fun Releasable.awaitBeforeRelease() {
    try {
        awaitCancellation()
    } finally {
        release()
    }
}

private fun backwardAnimation(maxWidth: Dp, pastTransitionProgress: Float = 1F): Animation {
    val animator = Animatable(initialValue = 1 - pastTransitionProgress)
    val progress = animator.asState()
    return Animation(
        animator = animator,
        duration = (pastTransitionProgress * ANIM_DURATION_SLIDE).toInt(),
        type = Type.BACKWARD,
        targetModifier = slideInFromLeft(maxWidth, progress),
        currentModifier = slideOutToRight(maxWidth, progress)
    )
}

private fun forwardAnimation(maxWidth: Dp): Animation {
    val animator = Animatable(initialValue = 0f)
    val progress = animator.asState()
    return Animation(
        animator = animator,
        duration = ANIM_DURATION_SLIDE,
        type = Type.FORWARD,
        targetModifier = slideInFromRight(maxWidth, progress),
        currentModifier = slideOutToLeft(maxWidth, progress)
    )
}

private fun replaceAnimation(): Animation {
    val animator = Animatable(initialValue = 0f)
    val progress = animator.asState()
    return Animation(animator = animator, duration = ANIM_DURATION_FADE, type = Type.REPLACE, targetModifier = fadeIn(progress), currentModifier = fadeOut(progress))
}

private fun fadeIn(progress: State<Float>): Modifier = Modifier.graphicsLayer { this.alpha = progress.value }
private fun fadeOut(progress: State<Float>): Modifier = Modifier.graphicsLayer { this.alpha = (1 - progress.value) }
private fun slideInFromRight(maxWidth: Dp, progress: State<Float>) = Modifier.graphicsLayer { this.translationX = (maxWidth * (1 - progress.value)).toPx() }
private fun slideOutToRight(maxWidth: Dp, progress: State<Float>) = Modifier.graphicsLayer { this.translationX = (maxWidth * progress.value).toPx() }

private fun slideInFromLeft(maxWidth: Dp, progress: State<Float>): Modifier {
    return Modifier
        .graphicsLayer { this.translationX = (maxWidth * FACTOR_TRANSLATION * (progress.value - 1)).toPx() }
        .drawWithContent { drawContent(); drawRect(color = Color.Black, alpha = (1 - progress.value) * FACTOR_OVERLAY) }
}

private fun slideOutToLeft(maxWidth: Dp, progress: State<Float>): Modifier {
    return Modifier
        .graphicsLayer { this.translationX = (-maxWidth * FACTOR_TRANSLATION * progress.value).toPx() }
        .drawWithContent { drawContent(); drawRect(color = Color.Black, alpha = progress.value * FACTOR_OVERLAY) }
}

@JvmInline
@Stable
private value class StablePane<S : Any>(val pane: Pane<S>)

private val StablePane<*>.id: String get() = pane.id

internal fun interface NodeHostScope {
    @Composable
    fun Render(content: @Composable (hostedNodes: Int?, render: @Composable (Int) -> Unit) -> Unit)
}

private data class ActivePane(val pane: StablePane<*>, val modifier: Modifier) {
    val id: String get() = pane.id
}

private data class Animation(
    val animator: Animatable<Float, AnimationVector1D>,
    val duration: Int,
    val type: Type,
    val targetModifier: Modifier,
    val currentModifier: Modifier
) {
    enum class Type { FORWARD, BACKWARD, REPLACE }
}

private class BackPressedCallback(
    private val backPressed: () -> Unit
) : BackHandler.Callback {
    override fun onBackStarted() = Unit
    override fun onBackProgressed(progress: Float) = Unit
    override fun onBackPressed() = backPressed()
    override fun onBackCancelled() = Unit
}
