package com.example.moviebox.ui.navigation

/**
 * Define todas las rutas de navegación de la app
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object TvShows : Screen("tvshows")
    object Games : Screen("games")
    object Watchlist : Screen("watchlist")
    object Profile : Screen("profile")
    object Search : Screen("search/{mediaType}") {
        fun createRoute(mediaType: String) = "search/$mediaType"
    }
    object MovieDetail : Screen("movie_detail/{movieId}") {
        fun createRoute(movieId: Int) = "movie_detail/$movieId"
    }
    object TvShowDetail : Screen("tvshow_detail/{tvShowId}") {
        fun createRoute(tvShowId: Int) = "tvshow_detail/$tvShowId"
    }
    object GameDetail : Screen("game_detail/{gameId}") {
        fun createRoute(gameId: Int) = "game_detail/$gameId"
    }
}
