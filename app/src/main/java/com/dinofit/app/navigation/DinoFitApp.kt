package com.dinofit.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.navigation.NavBackStackEntry
import com.dinofit.app.core.designsystem.Motion
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.NavIcons
import com.dinofit.app.feature.fact.FactDialog
import com.dinofit.app.feature.fact.FactViewModel
import com.dinofit.app.feature.home.HomeRoute
import com.dinofit.app.feature.walk.WalkRoute
import com.dinofit.app.feature.meals.MealsRoute
import com.dinofit.app.feature.profile.ProfileRoute
import com.dinofit.app.feature.ranks.RanksRoute
import com.dinofit.app.feature.workout.WorkoutRoute

private fun order(entry: NavBackStackEntry): Int =
    TopDestination.entries.indexOfFirst { it.route == entry.destination.route }

private fun isForward(from: NavBackStackEntry, to: NavBackStackEntry) = order(to) >= order(from)

private fun tabEnter(forward: Boolean): EnterTransition =
    fadeIn(tween(Motion.MEDIUM, delayMillis = 40, easing = Motion.EaseOut)) +
        slideInHorizontally(tween(280, easing = Motion.EaseOut)) { w -> if (forward) w / 14 else -w / 14 }

private fun tabExit(forward: Boolean): ExitTransition =
    fadeOut(tween(Motion.EXIT, easing = Motion.EaseIn)) +
        slideOutHorizontally(tween(Motion.EXIT, easing = Motion.EaseIn)) { w -> if (forward) -w / 30 else w / 30 }

/** 5 onglets en bas ; le profil s'ouvre depuis l'icône de l'accueil. */
enum class TopDestination(val route: String, val label: String, val icon: ImageVector, val inBar: Boolean = true) {
    HOME("home", "Aujourd'hui", NavIcons.Today),
    MEALS("meals", "Repas", NavIcons.Meals),
    WALK("walk", "Marche", NavIcons.Walk),
    WORKOUT("workout", "Séance", NavIcons.Workout),
    RANKS("ranks", "Rangs", NavIcons.Ranks),
    PROFILE("profile", "Profil", NavIcons.Profile, inBar = false),
}

@Composable
fun DinoFitApp(factViewModel: FactViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val fact by factViewModel.uiState.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    fun navigate(dest: TopDestination) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = DinoColors.Bg,
        bottomBar = {
            Column {
            HorizontalDivider(color = DinoColors.Line)
            NavigationBar(containerColor = DinoColors.Bg, tonalElevation = 0.dp) {
                TopDestination.entries.filter { it.inBar }.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest.route,
                        onClick = { navigate(dest) },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.label, style = DinoType.Label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DinoColors.AmberInk,
                            selectedTextColor = DinoColors.AmberInk,
                            indicatorColor = DinoColors.AmberSoft,
                            unselectedIconColor = DinoColors.InkFaint,
                            unselectedTextColor = DinoColors.InkFaint,
                        ),
                    )
                }
            }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.HOME.route,
            modifier = Modifier.padding(padding),
            // Onglet à droite : la page arrive de la droite ; onglet à gauche : de la gauche.
            enterTransition = { tabEnter(isForward(initialState, targetState)) },
            exitTransition = { tabExit(isForward(initialState, targetState)) },
            popEnterTransition = { tabEnter(isForward(initialState, targetState)) },
            popExitTransition = { tabExit(isForward(initialState, targetState)) },
        ) {
            composable(TopDestination.HOME.route) { HomeRoute(
                    onOpenWorkout = { navigate(TopDestination.WORKOUT) },
                    onOpenRanks = { navigate(TopDestination.RANKS) },
                    onOpenWalk = { navigate(TopDestination.WALK) },
                    onOpenMeals = { navigate(TopDestination.MEALS) },
                    onOpenProfile = { navigate(TopDestination.PROFILE) },
                ) }
            composable(TopDestination.MEALS.route) { MealsRoute() }
            composable(TopDestination.WALK.route) { WalkRoute() }
            composable(TopDestination.WORKOUT.route) { WorkoutRoute() }
            composable(TopDestination.RANKS.route) { RanksRoute() }
            composable(TopDestination.PROFILE.route) { ProfileRoute() }
        }
    }

    // Un fait au hasard sur les dinosaures à chaque ouverture.
    FactDialog(fact, onDismiss = factViewModel::dismiss)
}
