package com.wokdsem.kioto.engine

import androidx.compose.runtime.Composable
import com.wokdsem.kioto.Node
import com.wokdsem.kioto.Node.Navigator
import com.wokdsem.kioto.NodeContext
import com.wokdsem.kioto.NodeNav
import com.wokdsem.kioto.NodeSpec
import com.wokdsem.kioto.NodeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class NodeNavRunner(
    context: NodeContext,
    private val handleRootDismissTry: (NodeNav.(rootToken: NodeToken) -> (() -> Unit)?)?,
) : NodeNav() {

    private val contextSupplier = context.asContextSupplier()
    private val stack = ArrayDeque<NodeRecord>()
    private val job: Job = SupervisorJob()
    private val presentedStackRecords: ArrayDeque<PresentedStackRecord> = ArrayDeque()
    private var onRootDismissTry: (() -> Unit)? = null
    private var released: Boolean = false

    private val navActivePanes: MutableStateFlow<ActivePanes?> = MutableStateFlow(value = null)

    internal val activePanes: Flow<ActivePanes> get() = navActivePanes.filterNotNull()

    override fun setNavigation(rootNode: () -> NodeToken) {
        runOnUiThread {
            updateNav(type = NavigationType.REPLACE, release = { true }) {
                rootNode().asRecord(tag = Tag.ROOT)
            }
        }
    }

    override fun release() {
        runOnUiThread {
            if (released) return@runOnUiThread
            for (index in stack.indices.reversed()) {
                stack[index].release()
                if (presentedStackRecords.lastOrNull()?.stackIndex == index) presentedStackRecords.removeAt(presentedStackRecords.lastIndex).completable?.complete(Unit)
            }
            job.cancel()
            released = true
        }
    }

    private inline fun updateNav(type: NavigationType, release: (NodeRecord) -> Boolean, navigation: () -> NodeRecord?) {
        if (released) return

        val startingStackSize = stack.size
        var lastRemovedNode: NodeRecord? = null
        var lastPresentedStackRecordIndex: Int? = null
        while (stack.isNotEmpty() && release(stack.last())) {
            if (stack.size == 1 && type == NavigationType.POP) return onRootDismissTry?.invoke() ?: Unit
            if (type == NavigationType.REPLACE) lastPresentedStackRecordIndex = evaluatePresentedStack()
            lastRemovedNode = stack.removeAt(stack.lastIndex).also(NodeRecord::release)
            if (type != NavigationType.REPLACE) lastPresentedStackRecordIndex = evaluatePresentedStack()
        }

        val navigationNode = navigation()?.also { node ->
            if (stack.isEmpty()) onRootDismissTry = handleRootDismissTry?.invoke(this, node.nodeToken)
            stack += node
        }

        val transition = when {
            startingStackSize == 0 || type == NavigationType.REPLACE -> Transition.REPLACE
            stack.size > startingStackSize -> when {
                navigationNode?.tag == Tag.CHILD -> Transition.SIBLING
                presentedStackRecords.lastOrNull()?.stackIndex == stack.lastIndex -> Transition.PRESENT_STACK
                else -> Transition.BEGIN_STACK
            }

            stack.size < startingStackSize -> when {
                type != NavigationType.POP -> Transition.REPLACE
                lastRemovedNode?.tag == Tag.CHILD -> Transition.BACK
                lastPresentedStackRecordIndex == stack.size -> Transition.CLOSE_PRESENTED_STACK
                else -> Transition.CLOSE_STACK
            }

            else -> Transition.REPLACE
        }

        val background = when (stack.size) {
            0 -> error("Unreachable state: stack cannot be empty")
            1 -> if (onRootDismissTry == null) null else ActivePanes.HandledBackground
            else -> ActivePanes.BackgroundPane(stack[stack.lastIndex - 1].pane, presentedStackRecords.lastOrNull()?.stackIndex == stack.lastIndex)
        }
        navActivePanes.value = ActivePanes(foreground = stack.last().pane, background = background, transition = transition, activeIds = stack)
    }

    private fun evaluatePresentedStack(): Int? {
        if (presentedStackRecords.lastOrNull()?.stackIndex != stack.size) return null
        val stackRecord = presentedStackRecords.removeAt(presentedStackRecords.lastIndex)
        stackRecord.completable?.complete(Unit)
        return stackRecord.stackIndex
    }

    private fun NodeToken.asRecord(tag: Tag) = NodeRecord(tag = tag, nodeToken = this)

    override fun presentStack(stackRootNode: () -> NodeToken) {
        runOnUiThread { presentStack(stackRootNode, null) }
    }

    override suspend fun awaitPresentStack(stackRootNode: () -> NodeToken) {
        val completable = CompletableDeferred<Unit>()
        var stackIndex: Int? = null
        try {
            withContext(Dispatchers.Main) { stackIndex = presentStack(stackRootNode, completable) }
            completable.await()
        } catch (e: CancellationException) {
            withContext(NonCancellable + Dispatchers.Main) {
                if (stackIndex != null) updateNav(type = NavigationType.POP, release = { stack.size > stackIndex }, navigation = { null })
            }
            throw e
        }
    }

    private fun presentStack(stackRootNode: () -> NodeToken, completable: CompletableDeferred<Unit>?): Int {
        val stackIndex = stack.size
        presentedStackRecords += PresentedStackRecord(stackIndex, completable)
        updateNav(type = NavigationType.PUSH, { false }) {
            stackRootNode().asRecord(tag = if (stack.isEmpty()) Tag.ROOT else Tag.STACK)
        }
        return stackIndex
    }

    private inner class NodeNavigator(
        private val id: String
    ) : Navigator {

        private var released = false

        override fun navigate(node: () -> NodeToken) {
            runNavigation(NavigationType.PUSH, release = { false }) { node().asRecord(tag = if (stack.last().tag == Tag.ROOT) Tag.STACK else Tag.CHILD) }
        }

        override fun beginStack(stackRootNode: () -> NodeToken) {
            runNavigation(NavigationType.PUSH, release = { false }) { stackRootNode().asRecord(Tag.STACK) }
        }

        override fun replace(node: () -> NodeToken) {
            var replacedTag: Tag? = null
            runNavigation(
                type = NavigationType.REPLACE,
                release = { node -> (replacedTag == null || replacedTag == Tag.ROOT).also { replacedTag = node.tag } },
                navigation = { node().asRecord(tag = checkNotNull(replacedTag)) }
            )
        }

        override fun replaceStack(stackRootNode: () -> NodeToken) {
            var replacedTag: Tag? = null
            runNavigation(
                type = NavigationType.REPLACE,
                release = { node -> (replacedTag != Tag.STACK).also { replacedTag = node.tag } },
                navigation = { stackRootNode().asRecord(tag = Tag.STACK) }
            )
        }

        override fun navigateBack() {
            var backRun = false
            runNavigation(NavigationType.POP, release = { !backRun.also { backRun = true } })
        }

        override fun navigateUp() {
            var upRun = false
            runNavigation(NavigationType.POP, release = { node -> !upRun.also { upRun = node.tag != Tag.CHILD } })
        }

        override fun popToRoot() {
            var onRoot = false
            runNavigation(NavigationType.POP, release = { node -> (node.tag != Tag.ROOT || !onRoot).also { onRoot = true } })
        }

        private inline fun runNavigation(type: NavigationType, release: (NodeRecord) -> Boolean, navigation: () -> NodeRecord? = { null }) {
            runOnUiThread {
                if (released || this@NodeNavRunner.released) return@runOnUiThread
                var originAchieved = false
                updateNav(
                    type = type,
                    release = {
                        if (!originAchieved && it.id == id) originAchieved = true
                        !originAchieved || release(it)
                    },
                    navigation = navigation
                )
            }
        }

        override fun resetNavigation(rootNode: () -> NodeToken) = run { if (!released) this@NodeNavRunner.setNavigation(rootNode) }

        fun invalidate() {
            released = true
        }
    }

    private inner class NodeRecord(
        val nodeToken: NodeToken,
        val tag: Tag,
    ) : NavNodeId {

        override val id: String = Uuid.random().toString()
        val pane get() = identity.pane

        private val navigator = NodeNavigator(id)
        private val identity = NodeIdentity(id = id, spec = nodeToken.node().spec, parentJob = job, navigator = navigator, contextSupplier = contextSupplier)

        fun release() {
            navigator.invalidate()
            identity.release()
        }

    }

    private class NodeIdentity<N : Node<S>, S : Any>(id: String, spec: NodeSpec<N, S>, parentJob: Job, navigator: NodeNavigator, contextSupplier: ContextSupplier) {
        private val job = SupervisorJob(parentJob)
        private val state: MutableStateFlow<S> = MutableStateFlow(value = spec.state())
        private val hostedPanes: MutableStateFlow<Array<Pane<*>>?> = MutableStateFlow(value = null)
        private var hostedNodes: Array<NodeIdentity<*, *>>? = null
        private var released: Boolean = false

        val node = run {
            val scope = CoroutineScope(context = job + Dispatchers.Main.immediate)
            val updateState = { nextState: S -> runOnStableNode { state.value = nextState } }
            val state = { runOnStableNode { state.value } }
            val host = { nodes: Array<out NodeToken> ->
                runOnStableNode {
                    hostedNodes?.onEach(NodeIdentity<*, *>::release)
                    hostedNodes = Array(nodes.size) { index ->
                        NodeIdentity(id = Uuid.random().toString(), nodes[index].node().spec, job, navigator, contextSupplier)
                    }.also { nodes -> hostedPanes.value = if (nodes.isNotEmpty()) Array(nodes.size) { index -> nodes[index].pane } else null }
                }
            }
            val bundle = NodeHolder.Bundle(state = state, updateState = updateState, host = host, scope = scope, contextSupplier = contextSupplier, navigator = navigator)
            NodeHolder.hold(value = bundle, action = spec.node)
        }

        val pane: Pane<S> = run {
            val content = with(spec.view(node)) { @Composable { state: S -> Compose(state) } }
            Pane(id = id, state = state, navBack = navigator::navigateBack, hostedPanes = hostedPanes, content = content)
        }

        private inline fun <T> runOnStableNode(action: () -> T): T {
            checkOnUiThread()
            check(!released) { "Illegal state interaction on a released node" }
            return action()
        }

        fun release() {
            released = true
            job.cancel()
            node.clear()
        }
    }

    private enum class Tag { CHILD, STACK, ROOT }
    private enum class NavigationType { PUSH, REPLACE, POP }
    private class PresentedStackRecord(val stackIndex: Int, val completable: CompletableDeferred<Unit>?)

    internal enum class Transition { BEGIN_STACK, PRESENT_STACK, SIBLING, REPLACE, CLOSE_STACK, CLOSE_PRESENTED_STACK, BACK }

    internal interface NavNodeId {
        val id: String
    }

    internal class ActivePanes(val foreground: Pane<*>, val background: Background?, val transition: Transition, val activeIds: List<NavNodeId>) {
        sealed interface Background
        class BackgroundPane(val backgroundPane: Pane<*>, val presented: Boolean) : Background
        object HandledBackground : Background
    }

    internal class Pane<S : Any>(
        val id: PaneId,
        val state: StateFlow<S>,
        val hostedPanes: StateFlow<Array<Pane<*>>?>,
        val navBack: () -> Unit,
        val content: @Composable (S) -> Unit
    )

}

internal typealias PaneId = String
