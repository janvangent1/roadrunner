package com.roadrunner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.roadrunner.app.data.repository.AuthRepository
import com.roadrunner.app.navigation.RoadrunnerNavGraph
import com.roadrunner.app.navigation.Screen
import com.roadrunner.app.security.IntegrityChecker
import com.roadrunner.app.ui.theme.RoadrunnerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var integrityChecker: IntegrityChecker

    private var integrityBlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Skip integrity check in debug builds — emulators fail MEETS_BASIC_INTEGRITY
        if (!BuildConfig.DEBUG) {
            lifecycleScope.launch {
                try {
                    val passed = integrityChecker.check()
                    if (!passed) {
                        integrityBlocked = true
                    }
                } catch (e: Exception) {
                    // Network/API error — allow app to proceed (fail-open for v1)
                    android.util.Log.w("IntegrityChecker", "check failed with exception: ${e.message}")
                }
            }
        }

        setContent {
            RoadrunnerTheme {
                if (integrityBlocked) {
                    DeviceNotSupportedDialog()
                } else {
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
}

@Composable
private fun DeviceNotSupportedDialog() {
    AlertDialog(
        onDismissRequest = { /* non-dismissible — no-op */ },
        title = { Text("Device Not Supported") },
        text = { Text("This device does not meet the security requirements to run Roadrunner. Rooted or modified devices are not supported.") },
        confirmButton = { /* no buttons — fully blocks app */ },
    )
}
