package com.wokdsem.kioto.example

import com.wokdsem.kioto.NodeNav
import com.wokdsem.kioto.context
import com.wokdsem.kioto.example.deps.Logger
import com.wokdsem.kioto.example.features.landing.Landing
import com.wokdsem.kioto.example.features.logger
import com.wokdsem.kioto.provides

/**
 * Global navigation object for the example application.
 * Create as many nodeNav instances as you need, but this one is used in the example.
 * Use your preferred approach (di, global refs, ...) to hold nodeNav instances in your application.
 */
internal val nodeNav = NodeNav.newInstance(
    context = context(logger provides { Logger(::println) })
) { rootToken ->
    // Any attempt to dismiss a root node other than Landing, will take users to Landing node by setting a new navigation.
    // Having this way Landing as the only exit point for the application.
    if (rootToken != Landing.Token) return@newInstance { setNavigation { Landing.Token } } else null
}.apply {
    // Initialize the navigation with the first node.
    setNavigation { Landing.Token }
}
