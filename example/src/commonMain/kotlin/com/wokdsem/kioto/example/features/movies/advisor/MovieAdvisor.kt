package com.wokdsem.kioto.example.features.movies.advisor

import com.wokdsem.kioto.Node
import com.wokdsem.kioto.NodeToken
import com.wokdsem.kioto.example.data.suggestMovie
import com.wokdsem.kioto.example.domain.Movie
import com.wokdsem.kioto.example.features.logger
import com.wokdsem.kioto.example.features.movies.movie.MovieDetail

class MovieAdvisor : Node<MovieAdvisor.State>() {

    data class State(
        val movie: Movie? = null,
        val loadingNext: Boolean = false
    )

    object Token : NodeToken {
        override fun node() = node(::MovieAdvisor, ::State) {
            MovieAdvisorView(object : MovieAdvisorView.ViewListener {
                override fun onSeeMore(movie: Movie) = seeMore(movie)
                override fun onNext() = adviseNextMovie()
            })
        }
    }

    init {
        subscribe(::suggestMovie) { updateState { state.copy(movie = this) } }
    }

    private fun seeMore(movie: Movie) {
        nav.navigate { MovieDetail.Token(movie) }.also {
            logger().log("See more about ${movie.title}")
        }
    }

    private fun adviseNextMovie() {
        if (state.loadingNext || state.movie == null) return // Prevent multiple requests
        updateState { state.copy(loadingNext = true) }
        subscribe(source = ::suggestMovie) { updateState { state.copy(movie = this, loadingNext = false) } }
    }

}
