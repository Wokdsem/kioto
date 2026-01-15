package com.wokdsem.kioto.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wokdsem.kioto.LocalNodeScope
import com.wokdsem.kioto.example.ui.Screen.Navigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    title: String? = null,
    navigation: Navigation = Navigation.NONE,
    scrollable: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface {
        Column(modifier = Modifier.fillMaxSize().imePadding().statusBarsPadding()) {
            val nodeScope = LocalNodeScope.current
            if (title != null || navigation != Navigation.NONE) {
                CenterAlignedTopAppBar(
                    title = { if (title != null) Text(title) },
                    navigationIcon = {
                        when (navigation) {
                            Navigation.BACK -> IconButton(onClick = { nodeScope?.navigateBack() }) {
                                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                            }

                            Navigation.NONE -> Unit
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    windowInsets = WindowInsets()
                )
            }
            val scrollableModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
            Box(modifier = Modifier.fillMaxSize().weight(1f) then scrollableModifier) {
                content()
            }
        }
    }
}

class Screen {
    enum class Navigation { BACK, NONE }
}
