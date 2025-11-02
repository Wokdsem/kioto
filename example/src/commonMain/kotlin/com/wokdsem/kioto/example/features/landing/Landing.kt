package com.wokdsem.kioto.example.features.landing

import com.wokdsem.kioto.Node
import com.wokdsem.kioto.NodeToken
import com.wokdsem.kioto.example.features.about.About
import com.wokdsem.kioto.example.features.demo.Demo

class Landing : Node<Landing.State>() {

    object State

    object Token : NodeToken {
        override fun node() = node(::Landing, { State }) { LandingView() }
    }

    init {
        host(Demo.Token, About.Token)
    }

}