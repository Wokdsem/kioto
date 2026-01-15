package com.wokdsem.kioto.example.features.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wokdsem.kioto.NodeView
import com.wokdsem.kioto.example.ui.Screen

class NavigationView(
    private val stacks: List<List<Int>>,
    private val listener: ViewListener
) : NodeView<Navigation.State> {

    @Composable
    override fun Compose(state: Navigation.State) = NavigationScreen(stacks, listener)

    interface ViewListener {
        fun onNavigate()
        fun onBeginStack()
        fun onReplace()
        fun onReplaceStack()
        fun onNavigateBack()
        fun onNavigateUp()
        fun onPopToRoot()
        fun onResetNavigation()
    }
}

@Composable
private fun NavigationScreen(stacks: List<List<Int>>, listener: NavigationView.ViewListener) {
    fun Int.nodeId() = 'A' + this
    Screen(
        title = "Kioto navigation",
        navigation = Screen.Navigation.BACK
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            stacks.forEachIndexed { stackIndex, stack ->
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).background(Color.LightGray).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    stack.forEachIndexed { index, node ->
                        val color = if (stackIndex == stacks.lastIndex && index == stack.lastIndex) Color.Red else Color.Black
                        Text("${node.nodeId()}", modifier = Modifier.border(2.dp, color).background(Color.White).padding(16.dp), fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Column(modifier = Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Navigation("Navigate", listener::onNavigate)
                    Navigation("Begin stack", listener::onBeginStack)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Navigation("Replace", listener::onReplace)
                    Navigation("Replace stack", listener::onReplaceStack)
                }
                if (stacks.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Navigation("Navigate back", listener::onNavigateBack)
                        Navigation("Navigate up", listener::onNavigateUp)
                    }
                    Navigation("Pop to root", listener::onPopToRoot)
                }
                Navigation("Reset Navigation", listener::onResetNavigation)
            }
        }
    }
}

@Composable
private fun Navigation(navigation: String, onClick: () -> Unit) {
    Button(onClick) {
        Text(text = navigation, modifier = Modifier.padding(8.dp))
    }
}

@Preview
@Composable
private fun NavigationPreview() {
    NavigationScreen(
        stacks = listOf(listOf(1), listOf(2, 5), listOf(7)),
        listener = object : NavigationView.ViewListener {
            override fun onNavigate() = Unit
            override fun onBeginStack() = Unit
            override fun onReplace() = Unit
            override fun onReplaceStack() = Unit
            override fun onResetNavigation() = Unit
            override fun onNavigateBack() = Unit
            override fun onNavigateUp() = Unit
            override fun onPopToRoot() = Unit
        }
    )
}
