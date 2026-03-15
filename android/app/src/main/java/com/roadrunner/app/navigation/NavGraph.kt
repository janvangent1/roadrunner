package com.roadrunner.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.roadrunner.app.ui.auth.AuthViewModel
import com.roadrunner.app.ui.auth.LoginScreen
import com.roadrunner.app.ui.auth.RegisterScreen

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
            // Real CatalogScreen wired in Plan 04
            // Placeholder with sign out for now:
            val viewModel: AuthViewModel = hiltViewModel()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Catalog — Plan 04")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) { Text("Sign Out") }
                }
            }
        }

        composable(Screen.MyRoutes.route) {
            // Real MyRoutesScreen wired in Plan 04
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("My Routes — Plan 04")
            }
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
