package com.roadrunner.app.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Catalog : Screen("catalog")
    object MyRoutes : Screen("my_routes")
    object RouteDetail : Screen("route_detail/{routeId}") {
        fun createRoute(routeId: String) = "route_detail/$routeId"
    }
}
