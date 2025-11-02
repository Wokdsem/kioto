package com.wokdsem.kioto

import com.wokdsem.kioto.engine.NodeNavRunner
import kotlin.uuid.ExperimentalUuidApi

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
 */
@OptIn(ExperimentalUuidApi::class)
public abstract class NodeNav internal constructor() {

    public companion object {
        /**
         * Standard method to create a new instance of [NodeNav].
         *
         * @param context - A [NodeContext] that provides context to the nodes hosted by the [NodeNav] instance.
         * @param handleRootDismissTry - A lambda that receives the root [NodeToken] and returns an optional lambda to be invoked if the root node was attempted to be dismissed.
         */
        public fun newInstance(
            context: NodeContext = context(),
            handleRootDismissTry: (NodeNav.(rootToken: NodeToken) -> (() -> Unit)?)? = null,
        ): NodeNav = NodeNavRunner(context = context, handleRootDismissTry = handleRootDismissTry)
    }

    /**
     * If any, pops all the stacks and their nodes, and starts a new navigation stack with the specified root node.
     *
     * @param rootNode A lambda that, when invoked, returns a [NodeToken] representing the root node of the new navigation graph.
     */
    public abstract fun setNavigation(rootNode: () -> NodeToken)

    /**
     * Releases any active nodes and prevents any further navigation.
     * If the [NodeNav] instance's lifecycle is shorter than app's lifecycle, you must call this method to ensure proper cleanup.
     */
    public abstract fun release()

    /**
     * Presents a new node on top of any existing stacks containing the node returned by the [stackRootNode] lambda.
     *
     * @param stackRootNode - A lambda that returns a [NodeToken] representing the root node of the new stack to be presented.
     */
    public abstract fun presentStack(stackRootNode: () -> NodeToken)

    /**
     * Starts a new stack on top of any existing stacks containing the node returned by the [stackRootNode] lambda and suspends the execution until that presented node and
     * its subsequent replacements, if any, are removed from the navigation stack.
     *
     * @param stackRootNode - A lambda that returns a [NodeToken] representing the node to be presented.
     */
    public abstract suspend fun awaitPresentStack(stackRootNode: () -> NodeToken)

}
