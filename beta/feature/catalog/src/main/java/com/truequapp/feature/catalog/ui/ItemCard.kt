package com.truequapp.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.truequapp.core.designsystem.component.ConditionTag
import com.truequapp.core.designsystem.component.ItemCondition as DesignItemCondition
import com.truequapp.core.designsystem.theme.PaletteConfig
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.TradeItem

/**
 * Tarjeta de artículo para la cuadrícula del catálogo de marketplace (CU008 / Fase 2 / Warm Studio).
 * Muestra fotografía WebP mediante Coil, condición física, rango de valor estimado,
 * distancia difusa, indicador de múltiples fotos y marcador de favoritos con estilo Obsidian/Gold.
 */
@Composable
fun ItemCard(
    item: TradeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val designCondition = when (item.condition) {
        ItemCondition.NUEVO -> DesignItemCondition.NEW
        ItemCondition.COMO_NUEVO -> DesignItemCondition.LIKE_NEW
        ItemCondition.USADO -> DesignItemCondition.USED_GOOD
        ItemCondition.REGULAR -> DesignItemCondition.FAIR
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = PaletteConfig.ObsidianBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Imagen del ítem (WebP con Coil) o placeholder con relación de aspecto cuadrada 1:1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.imagesWebP.isNotEmpty()) {
                    AsyncImage(
                        model = item.imagesWebP.first(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = PaletteConfig.SmokyBronze.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Tag de condición física en la esquina superior izquierda
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    ConditionTag(condition = designCondition)
                }

                // Indicador de múltiples fotos WebP (si tiene más de 1)
                if (item.imagesWebP.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = PaletteConfig.GoldLight,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${item.imagesWebP.size}",
                                color = PaletteConfig.GoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Botón para borrar publicación (si es propia) o marcar como favorita
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB71C1C).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Borrar publicación",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (onToggleBookmark != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Quitar de guardados" else "Guardar publicación",
                                tint = if (isBookmarked) PaletteConfig.GoldChampagne else PaletteConfig.DarkOnBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Valor: \$${item.estimatedValueMin.toInt()} - \$${item.estimatedValueMax.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaletteConfig.GoldChampagne,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Distancia aproximada protegiendo la dirección real (RN-10) en Coatzacoalcos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PaletteConfig.GoldDim,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "~3.5 km • Coatzacoalcos",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaletteConfig.SmokyBronze,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
