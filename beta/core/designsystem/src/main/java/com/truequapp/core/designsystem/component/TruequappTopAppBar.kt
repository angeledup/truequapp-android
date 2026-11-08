package com.truequapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.PaletteConfig
import com.truequapp.core.designsystem.theme.TruequappTheme

/**
 * Standard Truequapp Top App Bar with embedded TruequeCoins balance and notification counter (Warm Studio Design).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruequappTopAppBar(
    title: String,
    coinsBalance: Int,
    eyebrow: String? = null,
    unreadNotifications: Int = 0,
    onCoinsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = PaletteConfig.SmokyBronze
                    )
                }
                Text(
                    text = title,
                    style = TruequappTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            // TruequeCoins Balance Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(PaletteConfig.GoldChampagne.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = PaletteConfig.GoldChampagne.copy(alpha = 0.40f),
                        shape = RoundedCornerShape(9999.dp)
                    )
                    .clickable(onClick = onCoinsClick)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(PaletteConfig.GoldChampagne),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PaletteConfig.DarkBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$coinsBalance TC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteConfig.GoldLight
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Notifications Icon with optional gold indicator dot
            IconButton(onClick = onNotificationsClick) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = PaletteConfig.SmokyBronze
                    )
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PaletteConfig.GoldChampagne)
                        )
                    }
                }
            }
        }
    )
}
