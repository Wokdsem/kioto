package com.wokdsem.kioto

import androidx.compose.runtime.Composable
import com.wokdsem.kioto.Node.Companion.node
import com.wokdsem.kioto.engine.ContextSupplier
import com.wokdsem.kioto.engine.NodeHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class NodeSpec<N : Node<S>, S : Any>(
    val node: () -> N, val state: () -> S, val view: N.() -> NodeView<S>
)

/**
 * Represents a token for navigating to a [com.wokdsem.kioto.Node].
 *
 * @see Node.node
 */
public interface NodeToken {
    /**
     * Returns the [Node] associated with this token. Instantiate a node by calling [com.wokdsem.kioto.Node.node].
     */
    public fun node(): Node

    /**
     * NodeToken holder.
     */
    public class Node internal constructor(
        internal val spec: NodeSpec<*, *>
    )
}

/**
 * Node UI supplier for [Node]. A new instance of [NodeView] is created when a [Node] is or becoming at the top of the stack and
 * destroyed at any other situation.
 *
 * @param S The type of the state that the node holds.
 */
public interface NodeView<S : Any> {
    /**
     * Composable function that renders the node's UI.
     *
     * @param state The current state of the node.
     */
    @Composable
    public fun Compose(state: S)
}

/**
 * A node represents a modular portion of your app's UI.
 * Manually instantiation of a node is not allowed as nodes can be only instantiated by the navigation system.
 * Wrap nodes as part of a [NodeToken] to navigate to them.
 *
 * @see NodeView
 * @see Navigator
 *
 * @param S The type of the state that the node holds.
 *
 * Nodes remain active as long as they are referenced by a [NodeNav] instance, node instances are not affected by system runtime configuration changes.
 *
 * Nodes act as state holders of the non-null type parameter [S]. States are automatically exposed to [NodeView] instances declared in the [NodeToken].
 * Nodes can mutate states in a safety way by calling [updateState] and read the current state by querying [state].
 *
 * A node can interact with the navigation system by interacting with its [nav] instance.
 *
 * All interactions with states and the navigator must happen in the UI thread, otherwise an [IllegalStateException] will be thrown.
 *
 * Instance of Node can subscribe to suspend functions and flows using [subscribe] and [flowSubscribe] respectively.
 * These subscriptions are cooperatively canceled when the node is cleared.
 */
public abstract class Node<S : Any> : ContextSupplier {

    public companion object {

        private val EMPTY = { }

        /**
         * Creates a [NodeToken.Node].
         *
         * @param node - Node lambda function builder
         * @param initialState - Lambda function that defines the initial state of the node.
         * @param view - Lambda function that instantiate a [NodeView] for the node.
         *
         * @return a node instance.
         */
        public fun <N : Node<S>, S : Any> node(
            node: () -> N, initialState: () -> S, view: N.() -> NodeView<S>
        ): NodeToken.Node = NodeToken.Node(spec = NodeSpec(node, initialState, view))

    }

    /**
     * @return the current state of the node.
     *
     * It must be called from the UI thread, otherwise an [IllegalStateException] will be thrown.
     */
    protected val state: S get() = nodeState()

    /**
     * @return the node navigator. Hosted nodes inherit navigator from their host node.
     */
    protected val nav: Navigator

    private val nodeState: () -> S
    private val updateState: (S) -> Unit
    private val host: (Array<out NodeToken>) -> Unit
    private val contextSupplier: ContextSupplier
    private val scope: CoroutineScope

    init {
        @Suppress("UNCHECKED_CAST")
        ((NodeHolder.value ?: error("Illegal node instantiation")) as NodeHolder.Bundle<S>).run {
            this@Node.nav = navigator
            this@Node.nodeState = state
            this@Node.host = host
            this@Node.updateState = updateState
            this@Node.contextSupplier = contextSupplier
            this@Node.scope = scope
        }
    }

    override fun <T> ProvidableContext<T>.invoke(): T {
        return contextSupplier.run { this@invoke.invoke() }
    }

    /**
     * Subscribes to a suspend function that returns [T] and handles the returned value or error.
     * The subscription is cooperative canceled when the node is cleared.
     *
     * @param T The type of the value returned by the source function.
     * @param source A suspend function that returns [T].
     * @param onError A lambda function that is called when an error occurs during the execution.
     * @param onCompleted A lambda function that is called with the result of the source function when it completes successfully.
     */
    protected fun <T> subscribe(
        source: suspend () -> T, onError: () -> Unit = EMPTY, onCompleted: T.() -> Unit
    ): Unit = flowSubscribe(source = { source.asFlow() }, onError, onCompleted)

    /**
     * Subscribes to a flow produced by a suspend function and handles the emitted values or errors.
     * The subscription is cooperative canceled when the node is cleared.
     *
     * @param T The type of the values emitted by the flow.
     * @param source A suspend function that returns a [Flow] of type [T].
     * @param onError A lambda function that is called when an error occurs during the execution.
     * @param onNext A lambda function that is called with each emitted value from the flow.
     */
    protected fun <T> flowSubscribe(
        source: suspend () -> Flow<T>, onError: () -> Unit = EMPTY, onNext: T.() -> Unit
    ) {
        fun postError() = run { if (scope.isActive) onError() }
        scope.launch {
            runCatching { withContext(context = Dispatchers.Default) { source() } }
                .fold(
                    onSuccess = { flow -> flow.flowOn(Dispatchers.IO).catch { postError() }.collect(onNext) },
                    onFailure = { postError() }
                )
        }
    }

    /**
     * Establishes this node as a parent for the specified child [nodes], allowing to create a hierarchical structure of nodes
     * where this node (the host) manages the lifecycle and navigation context of its children (hosted nodes).
     * A hosted node can in turn hosts its own children nodes.
     *
     * Key characteristics of hosted nodes:
     * - **Inherited Navigator:** Hosted nodes automatically inherit the [Navigator] instance from their host node.
     *   This means any navigation operations initiated from a hosted node will be forwarded to the host node.
     * - **Lifecycle Management:** Hosted nodes are automatically cleared when their host node is cleared.
     *
     * Calling [host] again will clear any previously hosted nodes and establish the new set of [nodes] as the children of this host.
     *
     * @param nodes A variable number of [NodeToken] instances representing the child nodes to be hosted.
     */
    protected fun host(vararg nodes: NodeToken) {
        host(nodes)
    }

    /**
     * Performs any cleanup actions when the node is cleared.
     */
    protected open fun onCleared(): Unit = Unit

    /**
     * Updates the node's state with the result of the provided [update] function.
     *
     * It must be called from the UI thread, otherwise an [IllegalStateException] will be thrown.
     *
     * @param update A lambda function that returns the new state for the node.
     */
    protected fun updateState(update: () -> S) {
        updateState(update())
    }

    internal fun clear() {
        onCleared()
    }

    /**
     * Available navigation operations for nodes.
     * @see NodeNav
     * @see NodeToken
     *
     * Unless navigation is initiated from the current active node (the node currently at the top of the top stack),
     * any subsequent stack and nodes will be cleared before the new navigation operation proceeds.
     */
    public interface Navigator {

        /**
         * Transitions to the specified node within the current stack.
         *
         * @param node A lambda that returns a [NodeToken] representing the node to navigate to.
         */
        public fun navigate(node: () -> NodeToken)

        /**
         * Starts a new stack with the specified root node.
         *
         * @param stackRootNode A lambda that returns a [NodeToken] representing the root node of the new stack.
         */
        public fun beginStack(stackRootNode: () -> NodeToken)

        /**
         * Replaces the node with the specified node.
         * This operation behaves as if the current node was atomically removed and a new node was added to the current stack except if the current node is a root node,
         * in which case, it behaves as if [resetNavigation] was called.
         *
         * @param node A lambda that returns a [NodeToken] representing the new node to replace the current one.
         */
        public fun replace(node: () -> NodeToken)

        /**
         * Starts a new stack replacing the node's current stack.
         * All nodes in the node's stack will be cleared, and a new stack will start with the specified root node.
         * Replacing a stack from a root node behaves as if [resetNavigation] was called.
         *
         * @param stackRootNode A lambda that returns a [NodeToken] representing the root node of the new stack.
         */
        public fun replaceStack(stackRootNode: () -> NodeToken)

        /**
         * Pops the node from the navigation stack. If the node is the root node of a stack, the stack will be cleared as well.
         */
        public fun navigateBack()

        /**
         * Pops all the nodes belonging to the node's stack and clears the stack.
         */
        public fun navigateUp()

        /**
         * Pops all the nodes from the stacks until the root node is reached.
         * Calling this method from a root node behaves as if [navigateBack] was called.
         */
        public fun popToRoot()

        /**
         * Pops all the stacks and their nodes and starts a new navigation stack with the specified root node.
         *
         * @param rootNode A lambda that returns a [NodeToken] representing the root node of the new navigation graph.
         */
        public fun resetNavigation(rootNode: () -> NodeToken)
    }

}
