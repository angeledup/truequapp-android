package com.truequapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.TruequappTheme

enum class KycLevel {
    LEVEL_1_PHONE,
    LEVEL_2_OFFICIAL_ID
}

/**
 * KYC Verification Badge component.
 * Displays level of user identity verification (Level 1: Phone, Level 2: Official ID).
 */
@Composable
fun TruequappBadge(
    level: KycLevel,
    modifier: Modifier = Modifier
) {
    val (label, icon, badgeColor) = when (level) {
        KycLevel.LEVEL_1_PHONE -> Triple(
            "Teléfono Verificado",
            Icons.Default.CheckCircle,
            TruequappTheme.colors.kycLevel1
        )
        KycLevel.LEVEL_2_OFFICIAL_ID -> Triple(
            "Identidad Oficial Verificada",
            Icons.Default.VerifiedUser,
            TruequappTheme.colors.kycLevel2
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = badgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
