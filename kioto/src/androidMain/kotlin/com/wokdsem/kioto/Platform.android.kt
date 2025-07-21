package com.wokdsem.kioto

import android.os.Looper

internal actual fun isOnUiThread(): Boolean = Thread.currentThread() === Looper.getMainLooper().thread
