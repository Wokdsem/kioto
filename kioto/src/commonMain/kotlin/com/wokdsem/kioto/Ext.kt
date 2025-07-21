package com.wokdsem.kioto

internal fun interface Releasable {
    fun release()
}

internal inline fun Releasable.use(block: Releasable.() -> Unit) {
    try {
        block()
    } finally {
        release()
    }
}
