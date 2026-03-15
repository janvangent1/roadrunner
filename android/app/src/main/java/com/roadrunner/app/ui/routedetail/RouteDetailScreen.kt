package com.roadrunner.app.ui.routedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.route?.title ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${uiState.error}")
            }

            uiState.route != null -> {
                val route = uiState.route!!
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    // Preview map
                    item {
                        OsmPreviewMap(
                            region = route.region,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                        )
                    }

                    // Metadata section
                    item {
                        Column(Modifier.padding(16.dp)) {
                            Text(route.title, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(route.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))

                            // Metadata chips row
                            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuggestionChip(onClick = {}, label = { Text(route.difficulty.name) })
                                SuggestionChip(onClick = {}, label = { Text(route.terrainType) })
                                SuggestionChip(onClick = {}, label = { Text(route.region) })
                                SuggestionChip(onClick = {}, label = { Text("${"%.1f".format(route.distanceKm)} km") })
                                SuggestionChip(onClick = {}, label = { Text("${route.estimatedDurationMinutes} min") })
                            }
                        }
                    }

                    // License status section
                    item {
                        LicenseStatusSection(
                            licenseStatus = uiState.licenseStatus,
                            licenseType = uiState.licenseType,
                            expiresAt = uiState.expiresAt,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    // Purchase options section (always shown)
                    item {
                        PurchaseOptionsSection(modifier = Modifier.padding(16.dp))
                    }

                    // Start Navigation button (disabled in Phase 3 — enabled Phase 5)
                    item {
                        Button(
                            onClick = { /* Phase 5 */ },
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text("Start Navigation")
                        }
                        Text(
                            "Navigation unlocked in a future update",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseStatusSection(
    licenseStatus: LicenseStatus,
    licenseType: LicenseType?,
    expiresAt: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text("License Status", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))

        when (licenseStatus) {
            LicenseStatus.AVAILABLE -> {
                Text(
                    "No license — contact us to purchase",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> {
                // Show license type
                val typeName = when (licenseType) {
                    LicenseType.DAY_PASS -> "Day Pass"
                    LicenseType.MULTI_DAY -> "Multi-Day Rental"
                    LicenseType.PERMANENT -> "Permanent"
                    null -> "Licensed"
                }
                Text(typeName, style = MaterialTheme.typography.bodyMedium)

                // Show expiry date if present
                if (expiresAt != null) {
                    val formatter = DateTimeFormatter
                        .ofPattern("dd MMM yyyy HH:mm", Locale.getDefault())
                        .withZone(ZoneId.systemDefault())
                    val formattedDate = try {
                        formatter.format(Instant.parse(expiresAt))
                    } catch (e: Exception) {
                        expiresAt
                    }
                    Text(
                        "Expires: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // Status-specific warning text
                when (licenseStatus) {
                    LicenseStatus.EXPIRED -> Text(
                        "License expired",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                    )
                    LicenseStatus.EXPIRING_SOON -> Text(
                        "Expires soon",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100), // orange
                    )
                    else -> { /* no additional warning */ }
                }
            }
        }
    }
}

@Composable
private fun PurchaseOptionsSection(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Purchase Options", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PurchaseOptionCard(
                title = "Day Pass",
                price = "€X.XX",
                description = "Valid 24 hours",
                modifier = Modifier.weight(1f, fill = false),
            )
            PurchaseOptionCard(
                title = "Multi-Day Rental",
                price = "€X.XX",
                description = "Select duration",
                modifier = Modifier.weight(1f, fill = false),
            )
            PurchaseOptionCard(
                title = "Permanent",
                price = "€X.XX",
                description = "Own forever",
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun PurchaseOptionCard(
    title: String,
    price: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(price, style = MaterialTheme.typography.headlineSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { /* v1: manual licensing — no action */ }) {
                Text("Contact to Purchase")
            }
        }
    }
}
