package com.example.moviebox.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moviebox.ui.screens.games.GamesScreen
import com.example.moviebox.ui.screens.gamedetail.GameDetailScreen
import com.example.moviebox.ui.screens.home.HomeScreen
import com.example.moviebox.ui.screens.login.LoginScreen
import com.example.moviebox.ui.screens.moviedetail.MovieDetailScreen
import com.example.moviebox.ui.screens.profile.ProfileScreen
import com.example.moviebox.ui.screens.search.SearchScreen
import com.example.moviebox.ui.screens.tvshowdetail.TvShowDetailScreen
import com.example.moviebox.ui.screens.tvshows.TvShowsScreen
import com.example.moviebox.ui.screens.watchlist.WatchlistScreen
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

/**
 * Items de la barra de navegación inferior
 */
data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

/**
 * Pantallas donde se muestra la barra inferior
 */
val bottomNavScreens = listOf(
    Screen.Home.route,
    Screen.TvShows.route,
    Screen.Games.route,
    Screen.Profile.route
)

/**
 * Configuración principal de navegación con barra inferior
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavScreens

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
            // Login
            composable(route = Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Películas (Home)
            composable(route = Screen.Home.route) {
                HomeScreen(
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToWatchlist = {
                        navController.navigate(Screen.Watchlist.route)
                    }
                )
            }

            // Series
            composable(route = Screen.TvShows.route) {
                TvShowsScreen(
                    onTvShowClick = { tvShowId ->
                        navController.navigate(Screen.TvShowDetail.createRoute(tvShowId))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToWatchlist = {
                        navController.navigate(Screen.Watchlist.route)
                    }
                )
            }

            // Videojuegos
            composable(route = Screen.Games.route) {
                GamesScreen(
                    onGameClick = { gameId ->
                        navController.navigate(Screen.GameDetail.createRoute(gameId))
                    }
                )
            }

            // Búsqueda
            composable(route = Screen.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    },
                    onTvShowClick = { tvShowId ->
                        navController.navigate(Screen.TvShowDetail.createRoute(tvShowId))
                    }
                )
            }

            // Watchlist
            composable(route = Screen.Watchlist.route) {
                WatchlistScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) },
                    onTvShowClick = { tvShowId -> navController.navigate(Screen.TvShowDetail.createRoute(tvShowId)) }
                )
            }

            // Perfil
            composable(route = Screen.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // Detalle película
            composable(
                route = Screen.MovieDetail.route,
                arguments = listOf(navArgument("movieId") { type = NavType.IntType })
            ) {
                MovieDetailScreen(onBack = { navController.popBackStack() })
            }

            // Detalle serie
            composable(
                route = Screen.TvShowDetail.route,
                arguments = listOf(navArgument("tvShowId") { type = NavType.IntType })
            ) {
                TvShowDetailScreen(onBack = { navController.popBackStack() })
            }

            // Detalle videojuego
            composable(
                route = Screen.GameDetail.route,
                arguments = listOf(navArgument("gameId") { type = NavType.IntType })
            ) {
                GameDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Películas", Screen.Home.route, Icons.Default.Movie),
        BottomNavItem("Series", Screen.TvShows.route, Icons.Default.Tv),
        BottomNavItem("Juegos", Screen.Games.route, Icons.Default.SportsEsports),
        BottomNavItem("Perfil", Screen.Profile.route, Icons.Default.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MovieBoxSurface,
        contentColor = MovieBoxOnBackground
    ) {
        items.forEach { item ->
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
    }
}
