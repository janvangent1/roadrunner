package com.roadrunner.app.ui.routedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import com.roadrunner.app.ui.theme.OrangePrimary
import com.roadrunner.app.ui.theme.OutlineColor
import com.roadrunner.app.ui.theme.SurfaceDark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    onStartNavigation: (routeId: String) -> Unit,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.navigationError) {
        val error = uiState.navigationError
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearNavigationError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.route?.title ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            routeId = viewModel.routeId,
                            routeRepository = viewModel.routeRepository,
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

                    // Start Navigation button
                    item {
                        val canNavigate = uiState.licenseStatus in listOf(
                            LicenseStatus.OWNED, LicenseStatus.ACTIVE, LicenseStatus.EXPIRING_SOON
                        )
                        Column(Modifier.padding(16.dp)) {
                            Button(
                                onClick = { viewModel.startNavigation { onStartNavigation(viewModel.routeId) } },
                                enabled = canNavigate && !uiState.isStartingNavigation,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangePrimary,
                                    contentColor = Color(0xFF0D0D0D),
                                ),
                            ) {
                                if (uiState.isStartingNavigation) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Start Navigation")
                                }
                            }
                            if (!canNavigate) {
                                Text(
                                    when (uiState.licenseStatus) {
                                        LicenseStatus.EXPIRED -> "License expired — contact us to renew"
                                        else -> "Purchase a license to start navigation"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            if (uiState.canDownloadTiles) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.downloadTiles() },
                                    enabled = !uiState.isDownloadingTiles,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isDownloadingTiles) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (uiState.isDownloadingTiles) "Queuing download..." else "Download for offline use")
                                }
                            } else if (uiState.isTilesCached) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Map tiles cached for offline use",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
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
        Text(
            text = "LICENSE STATUS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.em,
                color = OrangePrimary,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineColor),
        ) {
            Column(Modifier.padding(12.dp)) {
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
    }
}

@Composable
private fun PurchaseOptionsSection(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = "PURCHASE OPTIONS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.em,
                color = OrangePrimary,
            ),
        )
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
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineColor),
    ) {
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
