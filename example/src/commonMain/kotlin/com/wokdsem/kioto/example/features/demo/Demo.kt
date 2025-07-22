package com.wokdsem.kioto.example.features.demo

import com.wokdsem.kioto.Node
import com.wokdsem.kioto.NodeToken
import com.wokdsem.kioto.example.features.demo.Demo.State
import com.wokdsem.kioto.example.features.movies.advisor.MovieAdvisor
import com.wokdsem.kioto.example.features.navigation.Navigation

class Demo : Node<State>() {

    object State

    object Token : NodeToken {
        override fun node() = node(::Demo, { State }) {
            DemoView(object : DemoView.ViewListener {
                override fun onMovieAdvisor() = nav.navigate { MovieAdvisor.Token }
                override fun onNavigation() = nav.resetNavigation { Navigation.Token() }
            })
        }
    }

}
