package com.roadrunner.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun RoadrunnerNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen()
        }
        composable(Screen.Register.route) {
            RegisterScreen()
        }
        composable(Screen.Catalog.route) {
            CatalogScreen()
        }
        composable(Screen.MyRoutes.route) {
            MyRoutesScreen()
        }
        composable(
            route = Screen.RouteDetail.route,
            arguments = listOf(
                navArgument("routeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            RouteDetailScreen(routeId = routeId)
        }
    }
}

// Placeholder screen composables — replaced in Plans 03–06

@Composable
fun LoginScreen() {
    Box(contentAlignment = Alignment.Center) {
        Text("Login Screen — Plan 03")
    }
}

@Composable
fun RegisterScreen() {
    Box(contentAlignment = Alignment.Center) {
        Text("Register Screen — Plan 03")
    }
}

@Composable
fun CatalogScreen() {
    Box(contentAlignment = Alignment.Center) {
        Text("Catalog Screen — Plan 04")
    }
}

@Composable
fun MyRoutesScreen() {
    Box(contentAlignment = Alignment.Center) {
        Text("My Routes Screen — Plan 04")
    }
}

@Composable
fun RouteDetailScreen(routeId: String) {
    Box(contentAlignment = Alignment.Center) {
        Text("Route Detail Screen — routeId=$routeId — Plan 04")
    }
}
