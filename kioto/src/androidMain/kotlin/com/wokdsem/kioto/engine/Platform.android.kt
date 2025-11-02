package com.wokdsem.kioto.engine

import android.os.Looper

internal actual fun isOnUiThread(): Boolean = Thread.currentThread() === Looper.getMainLooper().thread
