package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ActiveStatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    if (isActive) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF44B700))
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}
