package com.roadrunner.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun RoadrunnerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}
