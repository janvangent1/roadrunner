package com.roadrunner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.roadrunner.app.navigation.RoadrunnerNavGraph
import com.roadrunner.app.navigation.Screen
import com.roadrunner.app.ui.theme.RoadrunnerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadrunnerTheme {
                val navController = rememberNavController()
                RoadrunnerNavGraph(
                    navController = navController,
                    startDestination = Screen.Login.route // Plan 02 will make this conditional
                )
            }
        }
    }
}
