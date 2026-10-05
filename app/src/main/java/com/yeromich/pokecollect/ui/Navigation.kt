package com.yeromich.pokecollect.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yeromich.pokecollect.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.ui.screens.CardDetailScreen
import com.yeromich.pokecollect.ui.screens.CatalogScreen
import com.yeromich.pokecollect.ui.screens.CollectionScreen
import com.yeromich.pokecollect.ui.screens.ScannerScreen
import com.yeromich.pokecollect.ui.screens.SetsScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Collection : Screen("collection", R.string.nav_collection, Icons.Filled.Inventory2)
    object Scanner : Screen("scanner", R.string.nav_scanner, Icons.Filled.CameraAlt)
    object Sets : Screen("sets", R.string.nav_sets, Icons.Filled.Style)
}

private val bottomBarScreens = listOf(Screen.Collection, Screen.Scanner, Screen.Sets)

private val bottomBarRoutes = bottomBarScreens.map { it.route }.toSet()

private const val TAB_FADE_MS = 150
private const val PUSH_ENTER_MS = 260
private const val PUSH_EXIT_MS = 200
private const val BAR_MS = 220

private val GLASS_BLUR = 24.dp
private const val GLASS_TINT_ALPHA = 0.55f
private const val GLASS_FALLBACK_ALPHA = 0.92f
private const val GLASS_NOISE = 0.05f
private const val GLASS_EDGE_ALPHA = 0.12f

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in bottomBarRoutes && targetState.destination.route in bottomBarRoutes

// Tabs are siblings, so they swap with a quick fade; deeper screens slide in from the side.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(fromEnd: Boolean): EnterTransition =
    if (isTabSwitch()) fadeIn(tween(TAB_FADE_MS))
    else slideInHorizontally(tween(PUSH_ENTER_MS, easing = LinearOutSlowInEasing)) { w -> if (fromEnd) w / 4 else -w / 4 } +
            fadeIn(tween(PUSH_ENTER_MS, easing = LinearOutSlowInEasing))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(toStart: Boolean): ExitTransition =
    if (isTabSwitch()) fadeOut(tween(TAB_FADE_MS))
    else slideOutHorizontally(tween(PUSH_EXIT_MS, easing = FastOutLinearInEasing)) { w -> if (toStart) -w / 4 else w / 4 } +
            fadeOut(tween(PUSH_EXIT_MS, easing = FastOutLinearInEasing))

private fun Card.toEncodedJson(): String {
    val json = Json.encodeToString(this)
    return URLEncoder.encode(json, "UTF-8").replace("+", "%20")
}

@Composable
fun PokeCollectApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route == Screen.Collection.route ||
            currentDestination?.route == Screen.Sets.route
    val hazeState = rememberHazeState()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(BAR_MS, easing = LinearOutSlowInEasing)) { it },
                exit = slideOutVertically(tween(BAR_MS, easing = FastOutLinearInEasing)) { it }
            ) {
                FloatingTabBar(
                    hazeState = hazeState,
                    selectedRoute = bottomBarScreens.firstOrNull { screen ->
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    }?.route,
                    onSelect = { screen ->
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
    ) { innerPadding ->
        // Tab screens scroll underneath the frosted bar and pad their own lists by its height;
        // deeper screens keep clear of the system navigation bar instead.
        val barHeight = innerPadding.calculateBottomPadding()
        val bottomInset = PaddingValues(bottom = if (showBottomBar) 0.dp else barHeight)
        NavHost(
            navController = navController,
            startDestination = Screen.Collection.route,
            modifier = Modifier
                .hazeSource(hazeState)
                .padding(bottomInset)
                .consumeWindowInsets(bottomInset),
            enterTransition = { enter(fromEnd = true) },
            exitTransition = { exit(toStart = true) },
            popEnterTransition = { enter(fromEnd = false) },
            popExitTransition = { exit(toStart = false) }
        ) {
            composable(Screen.Collection.route) {
                CollectionScreen(
                    bottomPadding = barHeight,
                    onCardClick = { card -> navController.navigate("card/${card.toEncodedJson()}") }
                )
            }
            composable(Screen.Scanner.route) {
                ScannerScreen(
                    onClose = { navController.popBackStack() },
                    onCardSelected = { card -> navController.navigate("card/${card.toEncodedJson()}") },
                    onSearchManual = { query ->
                        navController.navigate("catalog?setId=&setName=&query=${URLEncoder.encode(query, "UTF-8")}")
                    }
                )
            }
            composable(Screen.Sets.route) {
                SetsScreen(
                    bottomPadding = barHeight,
                    onSetClick = { set ->
                        val encodedName = URLEncoder.encode(set.name, "UTF-8")
                        navController.navigate("catalog?setId=${set.id}&setName=$encodedName")
                    },
                    onSearchClick = { navController.navigate("catalog") }
                )
            }
            composable(
                route = "catalog?setId={setId}&setName={setName}&query={query}",
                arguments = listOf(
                    navArgument("setId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("setName") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                CatalogScreen(
                    onCardClick = { card -> navController.navigate("card/${card.toEncodedJson()}") },
                    onNavigateBack = { navController.popBackStack() }
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

@Composable
private fun FloatingTabBar(hazeState: HazeState, selectedRoute: String?, onSelect: (Screen) -> Unit) {
    val glass = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .hazeEffect(state = hazeState) {
                blurRadius = GLASS_BLUR
                tints = listOf(HazeTint(glass.copy(alpha = GLASS_TINT_ALPHA)))
                noiseFactor = GLASS_NOISE
                // Below Android 12 there is no real blur, so lean on a denser tint.
                fallbackTint = HazeTint(glass.copy(alpha = GLASS_FALLBACK_ALPHA))
            }
            .border(0.5.dp, Color.White.copy(alpha = GLASS_EDGE_ALPHA), CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bottomBarScreens.forEach { screen ->
                TabItem(
                    screen = screen,
                    selected = screen.route == selectedRoute,
                    onClick = { onSelect(screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabItem(screen: Screen, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) colors.primaryContainer else Color.Transparent, tween(BAR_MS), label = "tabBg")
    val content by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(BAR_MS), label = "tabFg")
    val label = stringResource(screen.labelRes)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
    ) {
        Icon(screen.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = content)
    }
}
