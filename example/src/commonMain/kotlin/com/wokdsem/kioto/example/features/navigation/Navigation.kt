package com.wokdsem.kioto.example.features.navigation

import com.wokdsem.kioto.Node
import com.wokdsem.kioto.NodeToken

class Navigation : Node<Navigation.State>() {

    object State // Empty state

    class Token(val stacks: List<List<Int>> = listOf(listOf(0))) : NodeToken {
        override fun node() = node(::Navigation, { State }) {
            NavigationView(stacks, object : NavigationView.ViewListener {
                override fun onNavigate() = nav.navigate { Token(stacks = stacks.navigate()) }
                override fun onBeginStack() = nav.beginStack { Token(stacks = stacks.beginStack()) }
                override fun onReplace() = nav.replace { Token(stacks = stacks.replace()) }
                override fun onReplaceStack() = nav.replaceStack { Token(stacks = stacks.replaceStack()) }
                override fun onResetNavigation() = nav.resetNavigation { Token(stacks = listOf(listOf(stacks.next))) }
                override fun onNavigateBack() = nav.navigateBack()
                override fun onNavigateUp() = nav.navigateUp()
                override fun onPopToRoot() = nav.popToRoot()
            })
        }
    }

    private fun List<List<Int>>.beginStack() = toMutableList().apply { add(listOf(next)) }
    private fun List<List<Int>>.navigate() = toMutableList().apply { this[lastIndex] = this[lastIndex].toMutableList().apply { add(next) } }
    private fun List<List<Int>>.replace() = toMutableList().apply { this[lastIndex] = this[lastIndex].toMutableList().apply { this[lastIndex] = this@replace.next } }
    private fun List<List<Int>>.replaceStack() = toMutableList().apply { this[lastIndex] = listOf(next) }

    private val List<List<Int>>.next get() = last().last() + 1

}
