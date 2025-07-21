package com.wokdsem.kioto

import platform.Foundation.NSThread

internal actual fun isOnUiThread(): Boolean = NSThread.isMainThread
