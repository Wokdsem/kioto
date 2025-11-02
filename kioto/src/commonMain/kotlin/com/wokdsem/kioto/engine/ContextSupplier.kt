package com.wokdsem.kioto.engine

import com.wokdsem.kioto.NodeContext
import com.wokdsem.kioto.ProvidableContext

internal interface ContextSupplier {
    operator fun <T> ProvidableContext<T>.invoke(): T
}

internal fun NodeContext.asContextSupplier(): ContextSupplier {
    val store = values.associate { providedValue ->
        val factory = providedValue.factory
        providedValue.providableContext to lazy { factory() }
    }
    return object : ContextSupplier {
        @Suppress("UNCHECKED_CAST")
        override fun <T> ProvidableContext<T>.invoke(): T {
            val value = store[this] ?: error("No value provided for context: $this")
            return value.value as T
        }
    }
}
