package com.yeromich.pokecollect.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
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

// Phosphor icons: outline when idle, filled when the tab is current.
sealed class Screen(val route: String, val labelRes: Int, @DrawableRes val icon: Int, @DrawableRes val iconSelected: Int) {
    object Collection : Screen("collection", R.string.nav_collection, R.drawable.ic_cards, R.drawable.ic_cards_fill)
    object Scanner : Screen("scanner", R.string.nav_scanner, R.drawable.ic_scan, R.drawable.ic_scan_fill)
    object Sets : Screen("sets", R.string.nav_sets, R.drawable.ic_stack, R.drawable.ic_stack_fill)
}

private val bottomBarScreens = listOf(Screen.Collection, Screen.Scanner, Screen.Sets)

private val bottomBarRoutes = bottomBarScreens.map { it.route }.toSet()

private const val TAB_FADE_MS = 150
private const val PUSH_ENTER_MS = 260
private const val PUSH_EXIT_MS = 200
private const val BAR_MS = 220

private val GLASS_BLUR = 32.dp
private const val GLASS_TINT_ALPHA = 0.85f
private const val GLASS_FALLBACK_ALPHA = 0.92f
private const val GLASS_NOISE = 0.05f
private const val GLASS_EDGE_ALPHA = 0.16f
private const val TAB_IDLE_ALPHA = 0.7f
private const val TAB_PILL_ALPHA = 0.18f
private const val TAB_PRESSED_SCALE = 0.88f
private const val TAB_PRESS_MS = 120
private const val PILL_DAMPING = 0.75f
private val TAB_BAR_PADDING = 6.dp
private val TAB_GAP = 4.dp
private val TAB_ICON_SIZE = 22.dp
private val TAB_ICON_PADDING_H = 14.dp
private val TAB_ICON_PADDING_V = 3.dp
private val TAB_ICON_LABEL_GAP = 2.dp

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
    val selectedIndex = bottomBarScreens.indexOfFirst { it.route == selectedRoute }
    // One shared pill glides between tabs instead of each tab fading its own in and out.
    val pillPosition by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = PILL_DAMPING, stiffness = Spring.StiffnessMediumLow),
        label = "tabPill"
    )

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
        BoxWithConstraints(modifier = Modifier.padding(TAB_BAR_PADDING)) {
            val slot = (maxWidth - TAB_GAP * (bottomBarScreens.size - 1)) / bottomBarScreens.size
            if (selectedIndex >= 0) {
                // Mirrors a tab's layout with an invisible label, so the pill sits exactly behind the icon.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TAB_ICON_LABEL_GAP, Alignment.CenterVertically),
                    modifier = Modifier
                        .offset { IntOffset(((slot + TAB_GAP) * pillPosition).roundToPx(), 0) }
                        .width(slot)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = TAB_PILL_ALPHA), CircleShape)
                            .padding(TAB_ICON_PADDING_H, TAB_ICON_PADDING_V)
                            .size(TAB_ICON_SIZE)
                    )
                    Text(" ", style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TAB_GAP)) {
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
}

@Composable
private fun TabItem(screen: Screen, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // A small squeeze on the icon is the press feedback; a full-width ripple read as a hover block.
    val scale by animateFloatAsState(if (pressed) TAB_PRESSED_SCALE else 1f, tween(TAB_PRESS_MS), label = "tabPress")
    val icon by animateColorAsState(
        if (selected) colors.primary else colors.onSurface.copy(alpha = TAB_IDLE_ALPHA), tween(BAR_MS), label = "tabIcon"
    )
    val text by animateColorAsState(
        if (selected) colors.onSurface else colors.onSurface.copy(alpha = TAB_IDLE_ALPHA), tween(BAR_MS), label = "tabText"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TAB_ICON_LABEL_GAP, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        Icon(
            painterResource(if (selected) screen.iconSelected else screen.icon),
            contentDescription = null,
            tint = icon,
            modifier = Modifier
                .padding(TAB_ICON_PADDING_H, TAB_ICON_PADDING_V)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(TAB_ICON_SIZE)
        )
        Text(stringResource(screen.labelRes), style = MaterialTheme.typography.labelSmall, color = text)
    }
}
