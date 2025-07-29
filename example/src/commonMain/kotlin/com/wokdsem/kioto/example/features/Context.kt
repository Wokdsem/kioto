package com.wokdsem.kioto.example.features

import com.wokdsem.kioto.contextKeyOf
import com.wokdsem.kioto.example.deps.Logger

internal val logger = contextKeyOf<Logger>()
