package com.wokdsem.kioto.example.features.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wokdsem.kioto.NodeView
import com.wokdsem.kioto.example.ui.Screen
import com.wokdsem.kioto.host.PagerHost
import com.wokdsem.kioto.host.rememberPagerHostState

internal class LandingView : NodeView<Landing.State> {
    @Composable
    override fun Compose(state: Landing.State) = Landing(state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Landing(state: Landing.State) {
    Screen(title = "Kioto", scrollable = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            val pagerHostState = rememberPagerHostState()
            PrimaryTabRow(selectedTabIndex = pagerHostState.currentPage, modifier = Modifier.fillMaxWidth()) {
                repeat(2) { tabIndex ->
                    val tabText = if (tabIndex == 0) "Demo" else "About"
                    Tab(selected = pagerHostState.currentPage == tabIndex, onClick = { pagerHostState.currentPage = tabIndex }, text = { Text(text = tabText) })
                }
            }
            PagerHost(state = pagerHostState, modifier = Modifier.fillMaxSize().weight(1F))
        }
    }
}

@Preview
@Composable
private fun LandingPreview() {
    Landing(Landing.State)
}
