package com.roadrunner.app.ui.myroutes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadrunner.app.ui.catalog.RouteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRoutesScreen(
    onRouteClick: (routeId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: MyRoutesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Routes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isLoading) {
                item { CircularProgressIndicator() }
            }
            items(uiState.routes, key = { it.route.id }) { routeWithLicense ->
                RouteCard(
                    route = routeWithLicense,
                    onClick = { onRouteClick(routeWithLicense.route.id) },
                )
            }
            if (!uiState.isLoading && uiState.routes.isEmpty()) {
                item {
                    Text(
                        text = "No licensed routes yet.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
