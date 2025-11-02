package com.wokdsem.kioto.engine

import com.wokdsem.kioto.Node
import com.wokdsem.kioto.NodeToken
import kotlinx.coroutines.CoroutineScope

internal object NodeHolder {

    private var holder: Bundle<*>? = null

    val value get() = holder

    fun <R> hold(value: Bundle<*>, action: () -> R): R {
        holder = value
        return action().also { holder = null }
    }

    class Bundle<S : Any>(
        val state: () -> S,
        val updateState: (S) -> Unit,
        val host: (Array<out NodeToken>) -> Unit,
        val scope: CoroutineScope,
        val contextSupplier: ContextSupplier,
        val navigator: Node.Navigator
    )

}