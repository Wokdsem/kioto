package com.wokdsem.kioto.example

import com.wokdsem.kioto.NodeNav
import com.wokdsem.kioto.example.features.demo.Demo

/**
 * Global navigation object for the example application.
 * Create as many nodeNav instances as you need, but this one is used in the example.
 * Use your preferred approach (di, global refs, ...) to hold nodeNav instances in your application.
 */
internal val nodeNav = NodeNav { token ->
    // Any node other than Demo will have Demo as its parent.
    if (token != Demo.Token) Demo.Token else null
}.apply {
    // Initialize the navigation with the first node.
    setNavigation { Demo.Token }
}
