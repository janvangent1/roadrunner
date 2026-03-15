package com.roadrunner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.roadrunner.app.data.repository.AuthRepository
import com.roadrunner.app.navigation.RoadrunnerNavGraph
import com.roadrunner.app.navigation.Screen
import com.roadrunner.app.ui.theme.RoadrunnerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadrunnerTheme {
                val navController = rememberNavController()
                val startDestination = if (authRepository.isLoggedIn())
                    Screen.Catalog.route
                else
                    Screen.Login.route
                RoadrunnerNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                )
            }
        }
    }
}
