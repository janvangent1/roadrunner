package com.roadrunner.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.roadrunner.app.ui.auth.LoginScreen
import com.roadrunner.app.ui.auth.RegisterScreen
import com.roadrunner.app.ui.catalog.CatalogScreen
import com.roadrunner.app.ui.myroutes.MyRoutesScreen

@Composable
fun RoadrunnerNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Catalog.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Catalog.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        composable(Screen.Catalog.route) {
            CatalogScreen(
                onRouteClick = { routeId ->
                    navController.navigate(Screen.RouteDetail.createRoute(routeId))
                },
                onNavigateToMyRoutes = { navController.navigate(Screen.MyRoutes.route) },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.MyRoutes.route) {
            MyRoutesScreen(
                onRouteClick = { routeId ->
                    navController.navigate(Screen.RouteDetail.createRoute(routeId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.RouteDetail.route,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: return@composable
            // Real RouteDetailScreen wired in Plan 05
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Route Detail $routeId — Plan 05")
            }
        }
    }
}
