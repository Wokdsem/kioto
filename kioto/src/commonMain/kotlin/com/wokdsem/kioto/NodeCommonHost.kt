package com.wokdsem.kioto

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.wokdsem.kioto.Animation.Type
import com.wokdsem.kioto.NodeNav.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmInline

private const val ANIM_DURATION_SLIDE = 340
private const val ANIM_DURATION_FADE = 280
private const val FACTOR_OVERLAY = 0.16F
private const val FACTOR_TRANSLATION = 0.3F

private val LocalStateHolder = staticCompositionLocalOf<SaveableStateHolder> { error("Missing state holder") }

internal data class HostBundle(
    val navigation: NodeNav,
    val onStackCleared: () -> Unit,
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun NodeHost(bundle: HostBundle) {
    data class ActivePane(val pane: StablePane<*>, val modifier: Modifier) {
        val id: String get() = pane.id
    }

    val stateHolder = rememberSaveableStateHolder()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        var visiblePanes by remember { mutableStateOf<List<ActivePane>>(emptyList()) }
        val heldNodes = rememberSaveable { mutableListOf<String>() }
        LaunchedEffect(bundle) {
            val (nodeNav, onStackCleared, platform, backHandler) = bundle
            var pastTransitionProgress = 1F
            nodeNav.activePanes.dropWhile { it == null }.onEach { activePanes ->
                if (heldNodes.isNotEmpty() && heldNodes.last() == activePanes?.activeIds?.lastOrNull()?.id) return@onEach
                val refId = when (activePanes?.transition) {
                    Transition.BEGIN_STACK, Transition.SIBLING, Transition.REPLACE -> activePanes.activeIds.getOrNull(activePanes.activeIds.lastIndex - 1)
                    Transition.CLOSE_STACK, Transition.BACK -> activePanes.activeIds.lastOrNull()
                    null -> null
                }?.id
                while (heldNodes.isNotEmpty() && heldNodes.last() != refId) heldNodes.removeLast().let(stateHolder::removeState)
                if (activePanes?.transition?.run { this == Transition.BEGIN_STACK || this == Transition.SIBLING || this == Transition.REPLACE } == true) {
                    if (heldNodes.isEmpty() && activePanes.activeIds.size == 2) heldNodes += activePanes.activeIds.first().id
                    heldNodes += activePanes.activeIds.last().id
                }
            }.onEach { if (it == null) onStackCleared() }.mapNotNull { it }.mapLatest { activePanes ->
                val target = StablePane(pane = activePanes.foreground)
                val backPressedToken = backHandler.takeUnless { platform == Platform.IOS || activePanes.background == null }
                    ?.register(callback = BackPressedCallback(activePanes.foreground.navBack))
                try {
                    withContext(Dispatchers.IO) {
                        val noticeableBackward = visiblePanes.size == 2 && visiblePanes.first().id == target.id
                        val (animator, duration, type, targetModifier, currentModifier) = when {
                            noticeableBackward -> backwardAnimation(maxWidth, pastTransitionProgress)
                            visiblePanes.size != 1 -> replaceAnimation()
                            else -> when (activePanes.transition) {
                                Transition.BEGIN_STACK, Transition.SIBLING -> forwardAnimation(maxWidth)
                                Transition.CLOSE_STACK, Transition.BACK -> backwardAnimation(maxWidth)
                                Transition.REPLACE -> replaceAnimation()
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
                        }.onFailure {
                            pastTransitionProgress = animator.value
                        }.getOrThrow()
                    }
                } finally {
                    backPressedToken?.release()
                }

                val targetPanes = listOf(ActivePane(pane = target, modifier = Modifier))
                visiblePanes = targetPanes
                if (activePanes.background != null) {
                    val predictiveEvents: Channel<suspend () -> Unit> = Channel(capacity = Channel.BUFFERED, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                    val backPredictiveAnimator = Animatable(0F)
                    val predictiveBackToken = backHandler.register(object : BackHandler.Callback {
                        val predictivePanes = listOf(
                            ActivePane(pane = StablePane(activePanes.background), modifier = slideInFromLeft(maxWidth, backPredictiveAnimator.asState())),
                            ActivePane(pane = target, modifier = slideOutToRight(maxWidth, backPredictiveAnimator.asState()))
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
                } else {
                    pastTransitionProgress = 1F
                }
            }.collect()
        }
        CompositionLocalProvider(
            LocalStateHolder provides stateHolder,
            LocalPlatform provides bundle.platform,
            LocalBackHandler provides bundle.backHandler.takeIf { bundle.platform == Platform.ANDROID }
        ) {
            Layout(
                content = { visiblePanes.forEach { (pane, modifier) -> key(pane.id) { Box(modifier) { Render(pane) } } } },
                measurePolicy = { panes, constraints ->
                    layout(width = constraints.maxWidth, height = constraints.maxHeight) {
                        panes.forEach { it.measure(constraints).place(0, 0, 0f) }
                    }
                }
            )
        }
    }
}

@Composable
private fun <S : Any> Render(stablePane: StablePane<S>) {
    val pane = stablePane.pane
    LocalStateHolder.current.SaveableStateProvider(pane.id) {
        CompositionLocalProvider(
            LocalNodeNavigation provides NodeNavigation(pane.navBack)
        ) {
            val state by pane.state.collectAsState()
            pane.content(state)
        }
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
private value class StablePane<S : Any>(val pane: NodeNav.Pane<S>)

private val StablePane<*>.id: String get() = pane.id

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
