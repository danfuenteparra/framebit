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
    object EditProfile : Screen("edit_profile")
    object AddReview : Screen("add_review")

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
    object UserSearch : Screen("user_search")
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    object UserList : Screen("user_list/{userId}/{listType}") {
        fun createRoute(userId: String, listType: String) = "user_list/$userId/$listType"
    }
    object ReviewDetail : Screen("review_detail/{reviewId}") {
        fun createRoute(reviewId: String) = "review_detail/$reviewId"
    }
}