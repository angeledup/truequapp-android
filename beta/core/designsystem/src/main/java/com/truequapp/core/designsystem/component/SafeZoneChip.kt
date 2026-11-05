package com.truequapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.TruequappTheme

/**
 * SafeZoneChip indicator for geospatial safe exchange points.
 */
@Composable
fun SafeZoneChip(
    isVerified: Boolean,
    modifier: Modifier = Modifier,
    zoneName: String? = null
) {
    val chipColor = if (isVerified) TruequappTheme.colors.safeZoneVerified else TruequappTheme.colors.safeZoneUnverified
    val icon = if (isVerified) Icons.Default.GppGood else Icons.Default.GppMaybe
    val text = if (isVerified) {
        zoneName ?: "Punto Seguro Verificado"
    } else {
        "Punto No Oficial / Precaución"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .border(width = 1.dp, color = chipColor.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = chipColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = chipColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
