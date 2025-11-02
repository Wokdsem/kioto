package com.wokdsem.kioto.example.features.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wokdsem.kioto.NodeView
import com.wokdsem.kioto.example.ui.Screen
import org.jetbrains.compose.ui.tooling.preview.Preview

internal class DemoView(
    private val listener: ViewListener
) : NodeView<Demo.State> {

    @Composable
    override fun Compose(state: Demo.State) = Dashboard(state, listener)

    interface ViewListener {
        fun onMovieAdvisor()
        fun onNavigation()
    }
}

@Composable
private fun Dashboard(state: Demo.State, listener: DemoView.ViewListener) {
    Screen {
        Column(modifier = Modifier.fillMaxWidth().padding(all = 24.dp), verticalArrangement = Arrangement.spacedBy(space = 32.dp)) {
            DemoCard(title = "Movie Advisor", onClick = listener::onMovieAdvisor)
            DemoCard(title = "Kioto navigation", onClick = listener::onNavigation)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DemoCard(title: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = title, modifier = Modifier.padding(all = 32.dp))
    }
}

@Preview
@Composable
private fun DashboardPreview() {
    Dashboard(Demo.State, object : DemoView.ViewListener {
        override fun onMovieAdvisor() = Unit
        override fun onNavigation() = Unit
    })
}
