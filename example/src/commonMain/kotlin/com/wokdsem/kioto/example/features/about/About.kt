package com.wokdsem.kioto.example.features.about

import com.wokdsem.kioto.Node
import com.wokdsem.kioto.NodeToken

class About : Node<About.State>() {

    object State

    object Token : NodeToken {
        override fun node() = node(::About, { State }) { AboutView() }
    }

}
