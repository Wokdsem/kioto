package com.wokdsem.kioto.example.features.movies.movie

import com.wokdsem.kioto.Node
import com.wokdsem.kioto.NodeToken
import com.wokdsem.kioto.example.domain.Movie

class MovieDetail : Node<MovieDetail.State>() {

    object State

    class Token(val movie: Movie) : NodeToken {
        override fun node() = node(::MovieDetail, { State }) {
            MovieDetailView(movie = movie)
        }
    }

}
