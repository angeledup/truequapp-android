package com.truequapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
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
import com.truequapp.core.designsystem.theme.TruequappTheme

/**
 * Standard Truequapp Top App Bar with embedded TruequeCoins balance and notification counter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruequappTopAppBar(
    title: String,
    coinsBalance: Int,
    unreadNotifications: Int = 0,
    onCoinsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = TruequappTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            // TruequeCoins Balance Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(TruequappTheme.colors.truequeCoins.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = TruequappTheme.colors.truequeCoins.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(onClick = onCoinsClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "TruequeCoins Balance",
                        tint = TruequappTheme.colors.truequeCoins,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$coinsBalance TC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TruequappTheme.colors.truequeCoins
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Notifications Icon with optional badge
            IconButton(onClick = onNotificationsClick) {
                if (unreadNotifications > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = TruequappTheme.colors.error
                            ) {
                                Text(
                                    text = if (unreadNotifications > 99) "99+" else unreadNotifications.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TruequappTheme.colorScheme.background,
            titleContentColor = TruequappTheme.colorScheme.onBackground,
            actionIconContentColor = TruequappTheme.colorScheme.onBackground
        )
    )
}
