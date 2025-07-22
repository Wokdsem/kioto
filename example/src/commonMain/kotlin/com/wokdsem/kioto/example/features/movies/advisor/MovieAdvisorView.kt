package com.wokdsem.kioto.example.features.movies.advisor

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wokdsem.kioto.NodeView
import com.wokdsem.kioto.example.domain.Movie
import com.wokdsem.kioto.example.ui.Screen
import org.jetbrains.compose.ui.tooling.preview.Preview

class MovieAdvisorView(
    private val listener: ViewListener
) : NodeView<MovieAdvisor.State> {

    @Composable
    override fun Compose(state: MovieAdvisor.State) = MovieAdvisorScreen(state, listener)

    interface ViewListener {
        fun onSeeMore(movie: Movie)
        fun onNext()
    }
}

@Composable
private fun MovieAdvisorScreen(state: MovieAdvisor.State, listener: MovieAdvisorView.ViewListener) {
    Screen(
        title = "Movie advisor",
        navigation = Screen.Navigation.BACK
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(all = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(targetState = state.movie) { movie ->
                when (movie) {
                    null -> CircularProgressIndicator()
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Movie(movie) { listener.onSeeMore(movie) }
                            AnimatedContent(state.loadingNext) { loading ->
                                when (loading) {
                                    true -> CircularProgressIndicator()
                                    false -> Button(onClick = listener::onNext) { Text(text = "Next movie") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Movie(movie: Movie, onSeeMore: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f).border(1.dp, Color.Black).padding(16.dp), contentAlignment = Alignment.Center) {
                Text(text = "${movie.title.first()}", fontSize = 24.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = movie.title, fontWeight = FontWeight.SemiBold)
                Text(text = movie.description, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Rating: ${movie.rating}")
                    Text(text = "Year: ${movie.year}")
                }
            }
        }
        TextButton(onSeeMore, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = "See more")
        }
    }
}

@Preview
@Composable
private fun MovieAdvisorPreview() {
    MovieAdvisorScreen(
        state = MovieAdvisor.State(
            movie = Movie(id = "1", title = "Inception", description = "A mind-bending thriller about dreams within dreams.", rating = 8.4f, year = 2010)
        ),
        listener = object : MovieAdvisorView.ViewListener {
            override fun onSeeMore(movie: Movie) = Unit
            override fun onNext() = Unit
        }
    )
}
