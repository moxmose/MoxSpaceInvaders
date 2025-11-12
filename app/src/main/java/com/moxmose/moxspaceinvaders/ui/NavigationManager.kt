package com.moxmose.moxspaceinvaders.ui

import androidx.navigation.NavHostController

class NavigationManager {
    // Add navigation-related properties and methods here
    // navController should be injected or passed from outside
    private lateinit var navController: NavHostController

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigate(route: String) {
        navController.navigate(route)
    }

    fun navigateAndClear(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
    // Add other navigation-related methods here, e.g., popBackStack()
}