package com.wokdsem.kioto

import androidx.compose.runtime.Composable
import com.wokdsem.kioto.Node.Navigator
import com.wokdsem.kioto.NodeNav.NavNode.Tag
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [NodeNav] manages navigation among Nodes.
 * Navigation is in essence a data structure based on stacking stacks of [Node]. In this context,
 * a stack is a logical association among nodes. Because of this association, node groups can be popped or replaced
 * altogether at once.
 *
 * @see Node.Navigator
 *
 * Navigation is started by calling [NodeNav.setNavigation]. If a navigation was previously initiated,
 * the old navigation will be cleared and a new one started.
 * The node starting the navigation is known as the root node. A root node cannot share its stack with any other node, so any
 * navigation initiated from a root node will create a new stack.
 *
 * If a ([NodeToken]) -> [NodeToken]? is provided when instantiating [NodeNav], this lambda will be executed to determine
 * the parent node of a root node. The parent node of a root node is also treated as a root node, meaning it will create its own independent
 * stack at the beginning of the navigation.
 * Since predictive back navigation is at the core of this navigation solution, this might be called right after setting the navigation.
 *
 * The parent node of a root node is also treated as a root node, meaning it will create its own independent
 * stack at the beginning of the navigation history and cannot share its stack with other nodes added at that level.
 *
 * @param rootParentSupplier - A lambda that receives the root [NodeToken] and returns an optional [NodeToken] identifying the parent node of the root node.
 */
@OptIn(ExperimentalUuidApi::class)
public class NodeNav(
    private val rootParentSupplier: (NodeToken) -> NodeToken? = { null }
) {

    private val stack: ArrayDeque<NavNode> = ArrayDeque()
    private var actualRootLoaded: Boolean = false
    private var activePanes: ActivePanes? = null

    private val observers: MutableMap<String, NodeNavObserver> = mutableMapOf()

    /**
     * If any, pops all the stacks and their nodes, and starts a new navigation stack with the specified root node.
     *
     * @param rootNode A lambda that, when invoked, returns a [NodeToken] representing the root node of the new navigation graph.
     */
    public fun setNavigation(rootNode: () -> NodeToken) {
        runOnUiThread {
            updateNav(transition = Transition.REPLACE, release = { true }) {
                rootNode().asNavNode(tag = Tag.ROOT)
            }
        }
    }

    private inline fun updateNav(transition: Transition, release: (NavNode) -> Boolean, navigation: () -> NavNode?) {
        val observers = observers.values

        while (stack.isNotEmpty() && release(stack.last())) {
            stack.removeLast().run {
                navigator.invalidate()
                onDestroy?.invoke()
                this.node.destroy()
                observers.forEach { observer -> observer.onClearedPane(id) }
            }
        }
        if (stack.isEmpty()) actualRootLoaded = false

        navigation()?.let { stack += it }
        if (stack.size < 2 && !actualRootLoaded) {
            val rootFeat = if (stack.isNotEmpty()) rootParentSupplier(stack.first().token) else null
            if (rootFeat != null) {
                stack.addFirst(element = rootFeat.asNavNode(tag = Tag.ROOT))
                actualRootLoaded = true
            }
        }
        activePanes = when (stack.size) {
            0 -> null
            1 -> ActivePanes(foreground = stack.last().asPane(), background = null, transition = transition)
            else -> ActivePanes(foreground = stack.last().asPane(), background = stack[stack.lastIndex - 1].asPane(), transition = transition)
        }
        observers.forEach { observer -> observer.onActivePanesChanged(activePanes) }
    }

    private fun NodeToken.asNavNode(tag: Tag, onDestroy: (() -> Unit)? = null): NavNode {
        val id = Uuid.random().toString()
        val navigator = NodeNavigator(id)
        val node = buildNode(token = node().token, navigator = navigator)
        return NavNode(id = id, tag = tag, token = this, navigator = navigator, node = node, onDestroy = onDestroy)
    }

    private fun <F : Node<S>, S : Any> buildNode(token: Token<F, S>, navigator: Navigator): Node<S> {
        return Node.bundle.hold(
            value = Node.Companion.Bundle(token.state, token.view, navigator),
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
        runOnUiThread {
            updateNav(Transition.BEGIN_STACK, { false }) {
                stackRootNode().asNavNode(tag = if (stack.isEmpty()) Tag.ROOT else Tag.STACK)
            }
        }
    }

    /**
     * Starts a new stack on top of any existing stacks containing the node returned by the [stackRootNode] lambda and suspends the execution until that presented node is
     * removed from the navigation stack.
     *
     * @param stackRootNode - A lambda that returns a [NodeToken] representing the node to be presented.
     */
    public suspend fun awaitPresentStack(stackRootNode: () -> NodeToken) {
        val completable = CompletableDeferred<Unit>()
        val token = stackRootNode()
        var navNode: NavNode? = null
        try {
            withContext(Dispatchers.Main) {
                navNode = token.asNavNode(tag = if (stack.isEmpty()) Tag.ROOT else Tag.STACK) { completable.complete(Unit) }
                updateNav(Transition.BEGIN_STACK, { false }) { navNode }
            }
            completable.await()
        } catch (e: CancellationException) {
            withContext(NonCancellable + Dispatchers.Main) { navNode?.navigator?.navigateUp() }
            throw e
        }
    }

    internal fun observeNavigation(observer: NodeNavObserver): Releasable {
        val id = Uuid.random().toString()
        observers[id] = observer
        if (activePanes != null) observer.onActivePanesChanged(activePanes)
        return Releasable { observers.remove(id) }
    }

    private inner class NodeNavigator(
        private val id: String
    ) : Navigator {

        private var released = false

        override fun navigate(node: () -> NodeToken) {
            runNavigation(Transition.SIBLING, release = { false }) { node().asNavNode(tag = if (stack.last().tag == Tag.ROOT) Tag.STACK else Tag.CHILD) }
        }

        override fun beginStack(stackRootNode: () -> NodeToken) {
            runNavigation(Transition.BEGIN_STACK, release = { false }) { stackRootNode().asNavNode(Tag.STACK) }
        }

        override fun replace(node: () -> NodeToken) {
            var replacedTag: Tag? = null
            runNavigation(
                transition = Transition.REPLACE,
                release = { node -> (replacedTag == null || replacedTag == Tag.ROOT).also { replacedTag = node.tag } },
                navigation = { node().asNavNode(tag = checkNotNull(replacedTag)) }
            )
        }

        override fun replaceStack(stackRootNode: () -> NodeToken) {
            var replacedTag: Tag? = null
            runNavigation(
                transition = Transition.REPLACE,
                release = { node -> (replacedTag != Tag.STACK).also { replacedTag = node.tag } },
                navigation = { stackRootNode().asNavNode(tag = Tag.STACK) }
            )
        }

        override fun navigateBack() {
            var backRun = false
            runNavigation(Transition.BACK, release = { !backRun.also { backRun = true } })
        }

        override fun navigateUp() {
            var upRun = false
            runNavigation(Transition.CLOSE_STACK, release = { node -> !upRun.also { upRun = node.tag != Tag.CHILD } })
        }

        override fun popToRoot() {
            var onRoot = false
            runNavigation(Transition.CLOSE_STACK, release = { node -> (node.tag != Tag.ROOT || !onRoot).also { onRoot = true } })
        }

        private inline fun runNavigation(transition: Transition, release: (NavNode) -> Boolean, navigation: () -> NavNode? = { null }) {
            runOnUiThread {
                if (released) return@runOnUiThread
                var originAchieved = false
                updateNav(
                    transition = transition,
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

    private class NavNode(
        val id: String,
        val tag: Tag,
        val token: NodeToken,
        val node: Node<*>,
        val navigator: NodeNavigator,
        val onDestroy: (() -> Unit)?
    ) {
        enum class Tag { CHILD, STACK, ROOT }
    }

    internal interface NodeNavObserver {
        fun onActivePanesChanged(activePanes: ActivePanes?)
        fun onClearedPane(paneId: PaneId)
    }

    internal enum class Transition { BEGIN_STACK, SIBLING, REPLACE, CLOSE_STACK, BACK }

    internal class ActivePanes(val foreground: Pane<*>, val background: Pane<*>?, val transition: Transition)

    internal class Pane<S : Any>(
        val id: PaneId,
        val state: StateFlow<S>,
        val navBack: () -> Unit,
        val content: @Composable (S) -> Unit
    )

}

internal typealias PaneId = String
