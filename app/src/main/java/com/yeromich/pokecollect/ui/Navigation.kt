package com.yeromich.pokecollect.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.ui.screens.CardDetailScreen
import com.yeromich.pokecollect.ui.screens.CatalogScreen
import com.yeromich.pokecollect.ui.screens.CollectionScreen
import com.yeromich.pokecollect.ui.screens.ScannerScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Collection : Screen("collection", R.string.nav_collection, Icons.Filled.Inventory2)
    object Scanner : Screen("scanner", R.string.nav_scanner, Icons.Filled.CameraAlt)
    object Catalog : Screen("catalog", R.string.nav_catalog, Icons.Filled.Search)
}

private val bottomBarScreens = listOf(Screen.Collection, Screen.Scanner, Screen.Catalog)

private fun Card.toEncodedJson(): String {
    val json = Json.encodeToString(this)
    return URLEncoder.encode(json, "UTF-8").replace("+", "%20")
}

@Composable
fun PokeCollectApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomBarScreens.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelRes)) },
                            label = { Text(stringResource(screen.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Collection.route
        ) {
            composable(Screen.Collection.route) {
                CollectionScreen(
                    onCardClick = { card -> navController.navigate("card/${card.toEncodedJson()}") }
                )
            }
            composable(Screen.Scanner.route) { ScannerScreen() }
            composable(Screen.Catalog.route) {
                CatalogScreen(
                    onCardClick = { card -> navController.navigate("card/${card.toEncodedJson()}") }
                )
            }
            composable(
                route = "card/{cardJson}",
                arguments = listOf(navArgument("cardJson") { type = NavType.StringType })
            ) {
                CardDetailScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
