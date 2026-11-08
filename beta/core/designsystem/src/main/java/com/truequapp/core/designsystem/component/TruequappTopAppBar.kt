package com.truequapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.PaletteConfig
import com.truequapp.core.designsystem.theme.TruequappTheme

/**
 * Standard Truequapp Top App Bar with embedded TruequeCoins balance and interactive notification menu (Warm Studio Design).
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
    var showMenu by remember { mutableStateOf(false) }
    var activeUnread by remember(unreadNotifications) { mutableStateOf(unreadNotifications) }

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

            // Notifications Icon with interactive DropdownMenu
            Box {
                IconButton(
                    onClick = {
                        showMenu = !showMenu
                        onNotificationsClick()
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showMenu) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = if (showMenu) PaletteConfig.GoldChampagne else PaletteConfig.SmokyBronze
                        )
                        if (activeUnread > 0) {
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

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .width(320.dp)
                        .background(PaletteConfig.DarkSurface)
                        .border(
                            width = 1.dp,
                            color = PaletteConfig.ObsidianBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = PaletteConfig.GoldChampagne,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notificaciones",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaletteConfig.DarkOnSurface
                            )
                        }
                        if (activeUnread > 0) {
                            Text(
                                text = "Marcar leídas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = PaletteConfig.GoldLight,
                                modifier = Modifier
                                    .clickable {
                                        activeUnread = 0
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = PaletteConfig.ObsidianBorder,
                        thickness = 1.dp
                    )

                    // Notification 1: Balance
                    NotificationMenuItem(
                        icon = Icons.Default.MonetizationOn,
                        iconTint = PaletteConfig.GoldChampagne,
                        title = "Balance disponible",
                        description = "Tienes $coinsBalance TruequeCoins (TC) para intercambios o trueques.",
                        time = "Ahora"
                    )

                    // Notification 2: Punto Seguro CETIS 79
                    NotificationMenuItem(
                        icon = Icons.Default.Security,
                        iconTint = PaletteConfig.SafeZoneVerified,
                        title = "Punto Seguro CETIS 79",
                        description = "Entrega con código QR en Caseta de Vigilancia o Domo Cívico.",
                        time = "Hoy"
                    )

                    // Notification 3: Nube y Sincronización
                    NotificationMenuItem(
                        icon = Icons.Default.Sync,
                        iconTint = PaletteConfig.PrimaryLight,
                        title = "Sincronización en la Nube",
                        description = "Tus ofertas y chats se respaldan automáticamente en tiempo real.",
                        time = "Hoy"
                    )
                }
            }
        }
    )
}

@Composable
private fun NotificationMenuItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    time: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Dismiss or action */ }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaletteConfig.DarkOnSurface
                )
                Text(
                    text = time,
                    fontSize = 10.sp,
                    color = PaletteConfig.SmokyBronze
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = PaletteConfig.DarkOnSurfaceVariant
            )
        }
    }
}
