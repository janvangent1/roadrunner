package com.roadrunner.app.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roadrunner.app.data.remote.dto.Difficulty
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.RouteWithLicense
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RouteCard(
    route: RouteWithLicense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
        // Map preview (only if center coords available)
        if (route.route.centerLat != null && route.route.centerLng != null) {
            RouteMapPreview(
                lat = route.route.centerLat,
                lng = route.route.centerLng,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left orange accent stripe
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )

            // Card content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Row 1: title + license status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = route.route.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LicenseStatusBadge(status = route.licenseStatus, expiresAt = route.expiresAt)
                }

                // Row 2: difficulty badge + region
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DifficultyBadge(difficulty = route.route.difficulty)
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = route.route.region,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Row 3: distance, duration, terrain
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Straighten,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${"%.1f".format(route.route.distanceKm)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${route.route.estimatedDurationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = route.route.terrainType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        } // end Column
    }
}

@Composable
private fun LicenseStatusBadge(
    status: LicenseStatus,
    expiresAt: String?,
) {
    val (label, containerColor, contentColor) = when (status) {
        LicenseStatus.OWNED -> Triple("Owned", Color(0xFF4CAF50), Color.White)
        LicenseStatus.ACTIVE -> Triple("Active", Color(0xFF2196F3), Color.White)
        LicenseStatus.EXPIRING_SOON -> Triple("Expires soon", Color(0xFFFF9800), Color.White)
        LicenseStatus.EXPIRED -> Triple("Expired", Color(0xFFF44336), Color.White)
        LicenseStatus.AVAILABLE -> Triple("Available", Color(0xFF9E9E9E), Color.White)
    }

    val showExpiry = expiresAt != null &&
        (status == LicenseStatus.ACTIVE || status == LicenseStatus.EXPIRING_SOON)

    val formattedExpiry = if (showExpiry && expiresAt != null) {
        val formatter = DateTimeFormatter
            .ofPattern("MMM d, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        "Expires ${formatter.format(Instant.parse(expiresAt))}"
    } else {
        null
    }

    Column(horizontalAlignment = Alignment.End) {
        SuggestionChip(
            onClick = {},
            label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = containerColor,
                labelColor = contentColor,
            ),
        )
        if (formattedExpiry != null) {
            Text(
                text = formattedExpiry,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val (label, containerColor) = when (difficulty) {
        Difficulty.EASY -> Pair("Easy", Color(0xFF4CAF50))
        Difficulty.MODERATE -> Pair("Moderate", Color(0xFFFFB300))
        Difficulty.HARD -> Pair("Hard", Color(0xFFFF5722))
        Difficulty.EXPERT -> Pair("Expert", Color(0xFFF44336))
    }

    AssistChip(
        onClick = {},
        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = Color.White,
        ),
    )
}
