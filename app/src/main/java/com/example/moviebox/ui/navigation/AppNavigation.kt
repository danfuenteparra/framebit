package com.example.moviebox.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moviebox.ui.screens.review.AddReviewScreen
import com.example.moviebox.ui.screens.editprofile.EditProfileScreen
import com.example.moviebox.ui.screens.games.GamesScreen
import com.example.moviebox.ui.screens.gamedetail.GameDetailScreen
import com.example.moviebox.ui.screens.home.HomeScreen
import com.example.moviebox.ui.screens.login.LoginScreen
import com.example.moviebox.ui.screens.moviedetail.MovieDetailScreen
import com.example.moviebox.ui.screens.profile.ProfileScreen
import com.example.moviebox.ui.screens.search.SearchScreen
import com.example.moviebox.ui.screens.tvshowdetail.TvShowDetailScreen
import com.example.moviebox.ui.screens.tvshows.TvShowsScreen
import com.example.moviebox.ui.screens.userlist.UserListScreen
import com.example.moviebox.ui.screens.userprofile.UserProfileScreen

import com.example.moviebox.ui.screens.userprofile.sections.UserWatchedScreen
import com.example.moviebox.ui.screens.userprofile.sections.UserReviewsScreen
import com.example.moviebox.ui.screens.userprofile.sections.UserWatchlistScreen
import com.example.moviebox.ui.screens.userprofile.sections.UserMediaListScreen
import com.example.moviebox.ui.screens.userprofile.sections.buildReviewId
import com.example.moviebox.ui.screens.usersearch.UserSearchScreen
import com.example.moviebox.ui.screens.watchlist.WatchlistScreen
import com.example.moviebox.ui.screens.reviewdetail.ReviewDetailScreen
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface


data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavScreens = listOf(
    Screen.Home.route,
    Screen.TvShows.route,
    Screen.Games.route,
    Screen.Profile.route
)

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavScreens

    // Helper local: navegar al detalle de una reseña por reviewId
    val navigateToReview: (String) -> Unit = { reviewId ->
        navController.navigate(Screen.ReviewDetail.createRoute(reviewId))
    }

    Scaffold(
        containerColor = MovieBoxBackground,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(route = Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Home.route) {
                HomeScreen(
                    onMovieClick = { id -> navController.navigate(Screen.MovieDetail.createRoute(id)) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.createRoute("movie")) },
                    onNavigateToWatchlist = { navController.navigate(Screen.Watchlist.route) },
                    onReviewClick = navigateToReview
                )
            }

            composable(route = Screen.TvShows.route) {
                TvShowsScreen(
                    onTvShowClick = { id -> navController.navigate(Screen.TvShowDetail.createRoute(id)) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.createRoute("tv")) },
                    onNavigateToWatchlist = { navController.navigate(Screen.Watchlist.route) },
                    onReviewClick = navigateToReview
                )
            }

            composable(route = Screen.Games.route) {
                GamesScreen(
                    onGameClick = { id -> navController.navigate(Screen.GameDetail.createRoute(id)) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.createRoute("game")) },
                    onNavigateToWatchlist = { navController.navigate(Screen.Watchlist.route) },
                    onReviewClick = navigateToReview
                )
            }

            composable(
                route = Screen.Search.route,
                arguments = listOf(navArgument("mediaType") { type = NavType.StringType })
            ) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { id -> navController.navigate(Screen.MovieDetail.createRoute(id)) },
                    onTvShowClick = { id -> navController.navigate(Screen.TvShowDetail.createRoute(id)) },
                    onGameClick = { id -> navController.navigate(Screen.GameDetail.createRoute(id)) }
                )
            }

            composable(route = Screen.Watchlist.route) {
                WatchlistScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { id -> navController.navigate(Screen.MovieDetail.createRoute(id)) },
                    onTvShowClick = { id -> navController.navigate(Screen.TvShowDetail.createRoute(id)) },
                    onGameClick = { id -> navController.navigate(Screen.GameDetail.createRoute(id)) }
                )
            }

            composable(route = Screen.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToFollowers = { id ->
                        navController.navigate(Screen.UserList.createRoute(id, "followers"))
                    },
                    onNavigateToFollowing = { id ->
                        navController.navigate(Screen.UserList.createRoute(id, "following"))
                    },
                    onNavigateToUserSearch = { navController.navigate(Screen.UserSearch.route) },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onMovieClick = { id -> navController.navigate(Screen.MovieDetail.createRoute(id)) },
                    onTvShowClick = { id -> navController.navigate(Screen.TvShowDetail.createRoute(id)) },
                    onGameClick = { id -> navController.navigate(Screen.GameDetail.createRoute(id)) }
                )
            }

            composable(route = Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(route = Screen.UserSearch.route) {
                UserSearchScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { id -> navController.navigate(Screen.UserProfile.createRoute(id)) }
                )
            }

            composable(
                route = Screen.UserProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) {
                UserProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToFollowers = { id ->
                        navController.navigate(Screen.UserList.createRoute(id, "followers"))
                    },
                    onNavigateToFollowing = { id ->
                        navController.navigate(Screen.UserList.createRoute(id, "following"))
                    },
                    onMovieClick = { id -> navController.navigate(Screen.MovieDetail.createRoute(id)) },
                    onTvShowClick = { id -> navController.navigate(Screen.TvShowDetail.createRoute(id)) },
                    onGameClick = { id -> navController.navigate(Screen.GameDetail.createRoute(id)) },
                    onNavigateToWatched = { id ->
                        navController.navigate(Screen.UserWatched.createRoute(id)) },
                    onNavigateToReviews = { id ->
                        navController.navigate(Screen.UserReviews.createRoute(id)) },
                    onNavigateToWatchlist = { id ->
                        navController.navigate(Screen.UserWatchlist.createRoute(id)) }
                )
            }
            composable(
                route = Screen.UserWatched.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                UserWatchedScreen(
                    userName = "",
                    onBack = { navController.popBackStack() },
                    onItemClick = { mediaType, mediaId ->
                        val reviewId = buildReviewId(userId, mediaType, mediaId)
                        navController.navigate(Screen.ReviewDetail.createRoute(reviewId))
                    },
                    onSeeAll = { mediaType ->
                        navController.navigate(
                            Screen.UserMediaList.createRoute(userId, "watched", mediaType)
                        )
                    }
                )
            }

            composable(
                route = Screen.UserReviews.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                UserReviewsScreen(
                    userName = "",
                    onBack = { navController.popBackStack() },
                    onItemClick = { mediaType, mediaId ->
                        val reviewId = buildReviewId(userId, mediaType, mediaId)
                        navController.navigate(Screen.ReviewDetail.createRoute(reviewId))
                    },
                    onSeeAll = { mediaType ->
                        navController.navigate(
                            Screen.UserMediaList.createRoute(userId, "reviews", mediaType)
                        )
                    }
                )
            }

            composable(
                route = Screen.UserWatchlist.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                UserWatchlistScreen(
                    userName = "",
                    onBack = { navController.popBackStack() },
                    onItemClick = { mediaType, mediaId ->
                        val reviewId = buildReviewId(userId, mediaType, mediaId)
                        navController.navigate(Screen.ReviewDetail.createRoute(reviewId))
                    },
                    onSeeAll = { mediaType ->
                        navController.navigate(
                            Screen.UserMediaList.createRoute(userId, "watchlist", mediaType)
                        )
                    }
                )
            }

            composable(
                route = Screen.UserMediaList.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                    navArgument("mediaType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                UserMediaListScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { mediaType, mediaId ->
                        val reviewId = buildReviewId(userId, mediaType, mediaId)
                        navController.navigate(Screen.ReviewDetail.createRoute(reviewId))
                    }
                )
            }


            composable(
                route = Screen.UserList.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("listType") { type = NavType.StringType }
                )
            ) {
                UserListScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { id -> navController.navigate(Screen.UserProfile.createRoute(id)) }
                )
            }

            composable(route = Screen.AddReview.route) {
                AddReviewScreen(
                    onBack = { navController.popBackStack() },
                    onReviewSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.MovieDetail.route,
                arguments = listOf(navArgument("movieId") { type = NavType.IntType })
            ) {
                MovieDetailScreen(
                    onBack = { navController.popBackStack() },
                    onReviewClick = navigateToReview
                )
            }

            composable(
                route = Screen.TvShowDetail.route,
                arguments = listOf(navArgument("tvShowId") { type = NavType.IntType })
            ) {
                TvShowDetailScreen(
                    onBack = { navController.popBackStack() },
                    onReviewClick = navigateToReview
                )
            }

            composable(
                route = Screen.GameDetail.route,
                arguments = listOf(navArgument("gameId") { type = NavType.IntType })
            ) {
                GameDetailScreen(
                    onBack = { navController.popBackStack() },
                    onReviewClick = navigateToReview
                )
            }

            composable(
                route = Screen.ReviewDetail.route,
                arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
            ) {
                ReviewDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToMedia = { mediaId, mediaType ->
                        val route = when (mediaType) {
                            "movie" -> Screen.MovieDetail.createRoute(mediaId)
                            "tv" -> Screen.TvShowDetail.createRoute(mediaId)
                            "game" -> Screen.GameDetail.createRoute(mediaId)
                            else -> return@ReviewDetailScreen
                        }
                        navController.navigate(route)
                    },
                    onNavigateToUser = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val left = listOf(
        BottomNavItem("Películas", Screen.Home.route, Icons.Default.Movie),
        BottomNavItem("Series", Screen.TvShows.route, Icons.Default.Tv)
    )
    val right = listOf(
        BottomNavItem("Juegos", Screen.Games.route, Icons.Default.SportsEsports),
        BottomNavItem("Perfil", Screen.Profile.route, Icons.Default.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MovieBoxSurface,
        contentColor = MovieBoxOnBackground
    ) {
        left.forEach { item ->
            BottomItem(item, currentDestination, navController)
        }

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate(Screen.AddReview.route) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MovieBoxPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Añadir reseña",
                        tint = MovieBoxBackground
                    )
                }
            },
            label = { Text("Reseñar", fontSize = MaterialTheme.typography.labelSmall.fontSize) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MovieBoxPrimary,
                unselectedIconColor = MovieBoxOnBackground,
                selectedTextColor = MovieBoxPrimary,
                unselectedTextColor = MovieBoxOnBackground.copy(alpha = 0.7f),
                indicatorColor = MovieBoxSurface
            )
        )

        right.forEach { item ->
            BottomItem(item, currentDestination, navController)
        }
    }
}

@Composable
private fun RowScope.BottomItem(
    item: BottomNavItem,
    currentDestination: androidx.navigation.NavDestination?,
    navController: NavHostController
) {
    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
    NavigationBarItem(
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label, fontSize = MaterialTheme.typography.labelSmall.fontSize) },
        selected = selected,
        onClick = {
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MovieBoxPrimary,
            selectedTextColor = MovieBoxPrimary,
            unselectedIconColor = MovieBoxOnBackground.copy(alpha = 0.5f),
            unselectedTextColor = MovieBoxOnBackground.copy(alpha = 0.5f),
            indicatorColor = MovieBoxPrimary.copy(alpha = 0.15f)
        )
    )
}