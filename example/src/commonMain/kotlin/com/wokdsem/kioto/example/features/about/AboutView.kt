package com.wokdsem.kioto.example.features.about

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wokdsem.kioto.NodeView
import com.wokdsem.kioto.example.ui.Screen
import org.jetbrains.compose.ui.tooling.preview.Preview

internal class AboutView : NodeView<About.State> {
    @Composable
    override fun Compose(state: About.State) = About(state)
}

@Composable
private fun About(state: About.State) {
    Screen {
        val aboutContent = """
            Kioto is a lightweight and robust ui framework that streamlines navigation management in Compose Multiplatform Mobile applications.
            Its structured approach promotes a clear separation of concerns and improves code maintainability across your mobile projects.

            Why Kioto?

            In the evolving landscape of Compose Multiplatform Mobile development, managing complex navigation flows and maintaining a clean architecture can be challenging.
            Kioto addresses these challenges by offering a lightweight and opinionated solution designed to:

            * **Simplify Navigation** Define your application's UI as modular Node components, making navigation intuitive and easy to reason about.
            * **Promote Clean Architecture** Enforce a strict separation of concerns, leading to more maintainable and testable code.
            * **Ensure Multiplatform Consistency** Provide a unified navigation API that works seamlessly across Android and iOS, reducing platform-specific boilerplate.
            * **Enhance State Management** Leverage a robust state management system within each Node, ensuring predictable UI updates and a responsive user experience.

            Kioto helps you build scalable and robust Compose Multiplatform Mobile applications with confidence and efficiency.

        """.trimIndent()
        Text(text = aboutContent, modifier = Modifier.fillMaxWidth().padding(all = 18.dp))
    }
}

@Preview
@Composable
private fun AboutPreview() {
    About()
}
