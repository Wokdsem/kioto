package com.wokdsem.kioto.engine

import platform.Foundation.NSThread

internal actual fun isOnUiThread(): Boolean = NSThread.isMainThread
