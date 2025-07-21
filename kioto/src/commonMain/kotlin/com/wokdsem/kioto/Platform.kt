package com.wokdsem.kioto

internal inline fun <T> runOnUiThread(action: () -> T): T {
    checkOnUiThread()
    return action()
}

internal fun checkOnUiThread() {
    check(isOnUiThread()) { "Illegal operation out of the UI thread" }
}

internal expect fun isOnUiThread(): Boolean
