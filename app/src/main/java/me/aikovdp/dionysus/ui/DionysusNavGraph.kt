package me.aikovdp.dionysus.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigation.suite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigation.suite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigation.suite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.aikovdp.dionysus.ui.screens.diary.DiaryScreen
import me.aikovdp.dionysus.ui.screens.movie.MovieDetailScreen
import me.aikovdp.dionysus.ui.screens.watchlist.WatchlistScreen

@OptIn(
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class
)
@Composable
@Preview
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val startDestination = DionysusDestinations.WATCHLIST_ROUTE
    val selectedDestination =
        navBackStackEntry?.destination?.route ?: startDestination
    val navActions = remember(navController) {
        DionysusNavigationActions(navController)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = navigationSuiteItems(selectedDestination, navActions::navigateTo)
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(DionysusDestinations.WATCHLIST_ROUTE) {
                WatchlistScreen(
                    navigateToMovieDetails = navActions::navigateToMovieDetail
                )
            }
            composable(DionysusDestinations.DIARY_ROUTE) {
                DiaryScreen(
                    navigateToMovieDetails = navActions::navigateToMovieDetail
                )
            }
            composable(DionysusDestinations.MOVIE_DETAIL_ROUTE) {
                MovieDetailScreen(
                    navigateUp = navController::navigateUp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
private fun navigationSuiteItems(
    selectedDestination: String,
    navigateToTopLevelDestination: (DionysusTopLevelDestination) -> Unit
): NavigationSuiteScope.() -> Unit = {
    TOP_LEVEL_DESTINATIONS.forEach { item ->
        val selected = selectedDestination == item.route
        item(
            selected = selected,
            onClick = { navigateToTopLevelDestination(item) },
            icon = {
                Icon(
                    if (selected) item.selectedIcon else item.unselectedIcon,
                    stringResource(item.iconTextId)
                )
            },
            label = { Text(stringResource(item.iconTextId)) }
        )
    }
}
