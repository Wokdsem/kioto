package com.wokdsem.kioto.example

import com.wokdsem.kioto.example.features.demo.Demo
import com.wokdsem.kioto.nodeHost

object ExampleIOsApplication {

    fun getNodeNavUIViewController() = nodeHost(navigation = nodeNav, onStackCleared = { nodeNav.setNavigation { Demo.Token } })

}