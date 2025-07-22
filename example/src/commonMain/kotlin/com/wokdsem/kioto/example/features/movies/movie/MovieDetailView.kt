package com.wokdsem.kioto.example.features.movies.movie

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wokdsem.kioto.NodeView
import com.wokdsem.kioto.example.domain.Movie
import com.wokdsem.kioto.example.ui.Screen
import org.jetbrains.compose.ui.tooling.preview.Preview

class MovieDetailView(
    private val movie: Movie
) : NodeView<MovieDetail.State> {
    @Composable
    override fun Compose(state: MovieDetail.State) = MovieDetailScreen(movie)
}

@Composable
private fun MovieDetailScreen(movie: Movie) {
    Screen(title = movie.title, navigation = Screen.Navigation.BACK) {
        Movie(movie)
    }
}

@Composable
private fun Movie(movie: Movie) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f).border(1.dp, Color.Black).padding(16.dp), contentAlignment = Alignment.Center) {
                Text(text = "${movie.title.first()}", fontSize = 24.sp)
            }
            Column(modifier = Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = movie.title, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Rating: ${movie.rating}")
                    Text(text = "Year: ${movie.year}")
                }
            }
        }
        Text(text = movie.description)
    }
}

@Preview
@Composable
private fun MovieDetailPreview() {
    MovieDetailScreen(
        movie = Movie(id = "1", title = "Inception", description = "A mind-bending thriller about dreams within dreams.", rating = 8.8f, year = 2010)
    )
}
