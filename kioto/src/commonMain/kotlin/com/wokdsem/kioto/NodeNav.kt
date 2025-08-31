package com.wokdsem.kioto

import androidx.compose.runtime.Composable
import com.wokdsem.kioto.Node.Navigator
import com.wokdsem.kioto.NodeNav.NavNode.Tag
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [NodeNav] manages navigation among Nodes.
 * Navigation is in essence a data structure based on stacking stacks of [Node]. In this context,
 * a stack is a logical association among nodes. Because of this association, node groups can be popped or replaced altogether at once.
 *
 * @see Node.Navigator
 *
 * Navigation is started by calling [NodeNav.setNavigation].
 * If a navigation was previously initiated, the old navigation will be cleared and a new one started.
 *
 * The node starting the navigation is known as the root node.
 * A root node is treated as an special node since it's not allowed to share its stack with any other nodes. Thus, any navigation initiated from a root node will create a new stack.
 *
 * Once a [NodeNav] is initialized by adding a first node, aka root node, this cannot be emptied anymore. Any attempt to empty the navigation by dismissing its root node is
 * effectless. However, if a [handleRootDismissTry] lambda was provided when instantiating [NodeNav], you will be given the chance to handle the attempt to dismiss the root node.
 * Since predictive back navigation is at the core of this navigation solution, beware that this lambda might be called right after setting the navigation.
 *
 * If a [NodeNav] instance's lifecycle is shorter than app's lifecycle, it is necessary to call [release] to ensure proper cleanup of active nodes.
 *
 * [NodeNav] has an associated [NodeContext] that provides context to the nodes hosted by the [NodeNav] instance.
 * [Node] can leverage the context to access dependencies, retrieve scoped configurations, or any other helpful operation that this flexible approach allows.
 *
 * @see NodeContext
 * @see ProvidedValue
 * @see ProvidableContext
 *
 *
 * @param context - A [NodeContext] that provides context to the nodes hosted by the [NodeNav] instance.
 * @param handleRootDismissTry - A lambda that receives the root [NodeToken] and returns an optional lambda to be invoked if the root node was attempted to be dismissed.
 */
@OptIn(ExperimentalUuidApi::class)
public class NodeNav(
    context: NodeContext = context(),
    private val handleRootDismissTry: (NodeNav.(rootToken: NodeToken) -> (() -> Unit)?)? = null,
) {

    private val contextSupplier = context.asContextSupplier()
    private val stack: ArrayDeque<NavNode> = ArrayDeque()
    private val presentedStackRecords: ArrayDeque<PresentedStackRecord> = ArrayDeque()
    private var onRootDismissTry: (() -> Unit)? = null
    private var released: Boolean = false

    private val navActivePanes: MutableStateFlow<ActivePanes?> = MutableStateFlow(value = null)

    internal val activePanes: Flow<ActivePanes> get() = navActivePanes.filterNotNull()

    /**
     * If any, pops all the stacks and their nodes, and starts a new navigation stack with the specified root node.
     *
     * @param rootNode A lambda that, when invoked, returns a [NodeToken] representing the root node of the new navigation graph.
     */
    public fun setNavigation(rootNode: () -> NodeToken) {
        runOnUiThread {
            updateNav(type = NavigationType.REPLACE, release = { true }) {
                rootNode().asNavNode(tag = Tag.ROOT)
            }
        }
    }

    /**
     * Releases any active nodes and prevents any further navigation.
     * If the [NodeNav] instance's lifecycle is shorter than app's lifecycle, you must call this method to ensure proper cleanup.
     */
    public fun release() {
        runOnUiThread {
            if (released) return@runOnUiThread
            for (index in stack.indices.reversed()) {
                stack[index].release()
                if (presentedStackRecords.lastOrNull()?.stackIndex == index) presentedStackRecords.removeLast().completable?.complete(Unit)
            }
            released = true
        }
    }

    private inline fun updateNav(type: NavigationType, release: (NavNode) -> Boolean, navigation: () -> NavNode?) {
        if (released) return

        val startingStackSize = stack.size
        var lastRemovedNode: NavNode? = null
        var lastPresentedStackRecordIndex: Int? = null
        while (stack.isNotEmpty() && release(stack.last())) {
            if (stack.size == 1 && type == NavigationType.POP) return onRootDismissTry?.invoke() ?: Unit
            if (type == NavigationType.REPLACE) lastPresentedStackRecordIndex = evaluatePresentedStack()
            lastRemovedNode = stack.removeLast().also(NavNode::release)
            if (type != NavigationType.REPLACE) lastPresentedStackRecordIndex = evaluatePresentedStack()
        }

        val navigationNode = navigation()?.also { node ->
            if (stack.isEmpty()) onRootDismissTry = handleRootDismissTry?.invoke(this, node.token)
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
            else -> ActivePanes.BackgroundPane(stack[stack.lastIndex - 1].asPane(), presentedStackRecords.lastOrNull()?.stackIndex == stack.lastIndex)
        }
        navActivePanes.value = ActivePanes(foreground = stack.last().asPane(), background = background, transition = transition, activeIds = stack)
    }

    private fun evaluatePresentedStack(): Int? {
        if (presentedStackRecords.lastOrNull()?.stackIndex != stack.size) return null
        val stackRecord = presentedStackRecords.removeLast()
        stackRecord.completable?.complete(Unit)
        return stackRecord.stackIndex
    }

    private fun NodeToken.asNavNode(tag: Tag): NavNode {
        val id = Uuid.random().toString()
        val navigator = NodeNavigator(id)
        val node = buildNode(token = node().token, navigator = navigator)
        return NavNode(id = id, tag = tag, token = this, navigator = navigator, node = node)
    }

    private fun <F : Node<S>, S : Any> buildNode(token: Token<F, S>, navigator: Navigator): Node<S> {
        return Node.bundle.hold(
            value = Node.Companion.Bundle(token.state, token.view, contextSupplier = contextSupplier, navigator),
            action = token.node
        )
    }

    private fun NavNode.asPane(): Pane<*> {
        fun <S : Any> asPane(node: Node<S>): Pane<S> {
            return Pane(
                id = id,
                state = node.nodeState,
                navBack = navigator::navigateBack,
                content = with(node.viewBuilder()) { { Compose(it) } }
            )
        }
        return asPane(node)
    }

    /**
     * Presents a new node on top of any existing stacks containing the node returned by the [stackRootNode] lambda.
     *
     * @param stackRootNode - A lambda that returns a [NodeToken] representing the root node of the new stack to be presented.
     */
    public fun presentStack(stackRootNode: () -> NodeToken) {
        runOnUiThread { presentStack(stackRootNode, null) }
    }

    /**
     * Starts a new stack on top of any existing stacks containing the node returned by the [stackRootNode] lambda and suspends the execution until that presented node and
     * its subsequent replacements, if any, are removed from the navigation stack.
     *
     * @param stackRootNode - A lambda that returns a [NodeToken] representing the node to be presented.
     */
    public suspend fun awaitPresentStack(stackRootNode: () -> NodeToken) {
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
            stackRootNode().asNavNode(tag = if (stack.isEmpty()) Tag.ROOT else Tag.STACK)
        }
        return stackIndex
    }

    private inner class NodeNavigator(
        private val id: String
    ) : Navigator {

        private var released = false

        override fun navigate(node: () -> NodeToken) {
            runNavigation(NavigationType.PUSH, release = { false }) { node().asNavNode(tag = if (stack.last().tag == Tag.ROOT) Tag.STACK else Tag.CHILD) }
        }

        override fun beginStack(stackRootNode: () -> NodeToken) {
            runNavigation(NavigationType.PUSH, release = { false }) { stackRootNode().asNavNode(Tag.STACK) }
        }

        override fun replace(node: () -> NodeToken) {
            var replacedTag: Tag? = null
            runNavigation(
                type = NavigationType.REPLACE,
                release = { node -> (replacedTag == null || replacedTag == Tag.ROOT).also { replacedTag = node.tag } },
                navigation = { node().asNavNode(tag = checkNotNull(replacedTag)) }
            )
        }

        override fun replaceStack(stackRootNode: () -> NodeToken) {
            var replacedTag: Tag? = null
            runNavigation(
                type = NavigationType.REPLACE,
                release = { node -> (replacedTag != Tag.STACK).also { replacedTag = node.tag } },
                navigation = { stackRootNode().asNavNode(tag = Tag.STACK) }
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

        private inline fun runNavigation(type: NavigationType, release: (NavNode) -> Boolean, navigation: () -> NavNode? = { null }) {
            runOnUiThread {
                if (released) return@runOnUiThread
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

        override fun resetNavigation(rootNode: () -> NodeToken) = run { if (!released) this@NodeNav.setNavigation(rootNode) }

        fun invalidate() {
            released = true
        }
    }

    internal open class NavNodeId(val id: String)

    private class NavNode(
        id: String,
        val tag: Tag,
        val token: NodeToken,
        val node: Node<*>,
        val navigator: NodeNavigator,
    ) : NavNodeId(id) {
        enum class Tag { CHILD, STACK, ROOT }

        fun release() {
            navigator.invalidate()
            node.destroy()
        }
    }

    private enum class NavigationType { PUSH, REPLACE, POP }
    private class PresentedStackRecord(val stackIndex: Int, val completable: CompletableDeferred<Unit>?)

    internal enum class Transition { BEGIN_STACK, PRESENT_STACK, SIBLING, REPLACE, CLOSE_STACK, CLOSE_PRESENTED_STACK, BACK }

    internal class ActivePanes(val foreground: Pane<*>, val background: Background?, val transition: Transition, val activeIds: List<NavNodeId>) {
        sealed interface Background
        class BackgroundPane(val backgroundPane: Pane<*>, val presented: Boolean) : Background
        object HandledBackground : Background
    }

    internal class Pane<S : Any>(
        val id: PaneId,
        val state: StateFlow<S>,
        val navBack: () -> Unit,
        val content: @Composable (S) -> Unit
    )

}

internal typealias PaneId = String
