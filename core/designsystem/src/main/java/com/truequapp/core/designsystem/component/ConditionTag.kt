package com.truequapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.TruequappTheme

enum class ItemCondition {
    NEW,
    LIKE_NEW,
    USED_GOOD,
    FAIR
}

/**
 * Visual badge indicating item condition in catalog and trade proposals.
 */
@Composable
fun ConditionTag(
    condition: ItemCondition,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (condition) {
        ItemCondition.NEW -> "Nuevo" to TruequappTheme.colors.conditionNew
        ItemCondition.LIKE_NEW -> "Como nuevo" to TruequappTheme.colors.conditionLikeNew
        ItemCondition.USED_GOOD -> "Usado" to TruequappTheme.colors.conditionUsed
        ItemCondition.FAIR -> "Regular" to TruequappTheme.colors.conditionFair
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
