package com.truequapp.feature.swipe.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.truequapp.core.designsystem.theme.PaletteConfig
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.feature.swipe.mvi.SwipeUiEffect
import com.truequapp.feature.swipe.mvi.SwipeUiEvent
import com.truequapp.feature.swipe.mvi.SwipeUiState
import com.truequapp.feature.swipe.mvi.SwipeViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    viewModel: SwipeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToProposal: (targetItemId: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SwipeUiEffect.NavigateToProposal -> {
                    onNavigateToProposal(effect.targetItemId)
                }
                is SwipeUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SwipeUiEffect.TriggerMatchCelebration -> {
                    // Feedback háptico o celebración
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Modo Swipe (CU017)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (state.hasRemainingItems) {
                                "Publicación ${state.currentIndex + 1} de ${state.itemsDeck.size}"
                            } else "Sin artículos pendientes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(SwipeUiEvent.LoadFeed(state.currentUserId)) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reiniciar Exploración"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (!state.hasRemainingItems) {
                EmptyDeckView(
                    onReload = { viewModel.onEvent(SwipeUiEvent.LoadFeed(state.currentUserId)) },
                    onBack = onNavigateBack
                )
            } else {
                CardStackView(
                    state = state,
                    onSwipeRight = { item -> viewModel.onEvent(SwipeUiEvent.SwipeRight(item)) },
                    onSwipeLeft = { item -> viewModel.onEvent(SwipeUiEvent.SwipeLeft(item)) }
                )
            }

            if (state.showMatchDialog && state.matchedItem != null) {
                MatchCelebrationDialog(
                    matchedItem = state.matchedItem!!,
                    onDismiss = { viewModel.onEvent(SwipeUiEvent.DismissMatchDialog) },
                    onProposeTrade = {
                        viewModel.onEvent(SwipeUiEvent.ProposeTradeFromMatch(state.matchedItem!!.id))
                    }
                )
            }
        }
    }
}

@Composable
fun CardStackView(
    state: SwipeUiState,
    onSwipeRight: (TradeItem) -> Unit,
    onSwipeLeft: (TradeItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val topItem = state.currentTopItem ?: return
    val nextItem = state.itemsDeck.getOrNull(state.currentIndex + 1)

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(state.currentIndex) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Stack Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Fondo / Siguiente tarjeta
            if (nextItem != null) {
                SwipeCardContent(
                    item = nextItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 4.dp)
                        .scale(0.94f)
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                )
            }

            // Tarjeta superior interactiva
            val rotationZ = (offsetX.value / 60f).coerceIn(-25f, 25f)

            SwipeCardContent(
                item = topItem,
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .rotate(rotationZ)
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .pointerInput(topItem.id) {
                        detectDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (offsetX.value > 300f) {
                                        // Swipe Right (Me interesa)
                                        offsetX.animateTo(1200f, tween(250))
                                        onSwipeRight(topItem)
                                    } else if (offsetX.value < -300f) {
                                        // Swipe Left (Pasar)
                                        offsetX.animateTo(-1200f, tween(250))
                                        onSwipeLeft(topItem)
                                    } else {
                                        // Regresar al centro
                                        launch { offsetX.animateTo(0f, tween(200)) }
                                        launch { offsetY.animateTo(0f, tween(200)) }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }
                            }
                        )
                    },
                overlayBadge = {
                    if (offsetX.value > 60f) {
                        val alpha = ((offsetX.value - 60f) / 180f).coerceIn(0f, 1f)
                        SwipeBadge(
                            text = "ME INTERESA",
                            color = PaletteConfig.PrimaryDefault,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(24.dp)
                                .rotate(-15f),
                            alpha = alpha
                        )
                    } else if (offsetX.value < -60f) {
                        val alpha = ((-offsetX.value - 60f) / 180f).coerceIn(0f, 1f)
                        SwipeBadge(
                            text = "PASAR",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .rotate(15f),
                            alpha = alpha
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controles de Acción Inferiores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón PASAR (Cruz roja)
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        offsetX.animateTo(-1000f, tween(250))
                        onSwipeLeft(topItem)
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .shadow(6.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Pasar artículo",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Botón INFO / CENTRO
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        offsetX.animateTo(0f, tween(150))
                        offsetY.animateTo(0f, tween(150))
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .shadow(4.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Centrar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Botón ME INTERESA (Corazón / Trueque verde)
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        offsetX.animateTo(1000f, tween(250))
                        onSwipeRight(topItem)
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .shadow(6.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(2.dp, PaletteConfig.PrimaryDefault.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Me interesa artículo",
                    tint = PaletteConfig.PrimaryDefault,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun SwipeCardContent(
    item: TradeItem,
    modifier: Modifier = Modifier,
    overlayBadge: @Composable BoxScope.() -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Imagen con aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val imageUrl = item.imagesWebP.firstOrNull()
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(72.dp)
                                .align(Alignment.Center)
                        )
                    }

                    // Banda de Valor Estimado (RN-02)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "Est. \$${item.estimatedValueMin.toInt()} - \$${item.estimatedValueMax.toInt()} TC",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Información del Artículo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        AssistChip(
                            onClick = {},
                            label = { Text(item.condition.displayName) }
                        )
                    }

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Wishlist Tags (Qué busca a cambio)
                    if (item.tagsWishlist.isNotEmpty()) {
                        Text(
                            text = "Busca a cambio:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PaletteConfig.SecondaryDefault
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item.tagsWishlist.take(3).forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Insignia overlay de Me Interesa / Pasar
            overlayBadge()
        }
    }
}

@Composable
fun SwipeBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Box(
        modifier = modifier
            .border(3.dp, color.copy(alpha = alpha), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.9f * alpha), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color.copy(alpha = alpha),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
fun EmptyDeckView(
    onReload: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = null,
            tint = PaletteConfig.PrimaryDefault,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Al día con las publicaciones!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Has explorado todas las opciones disponibles en tu zona según tus filtros.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onReload,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Explorar de Nuevo")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al Catálogo")
        }
    }
}

@Composable
fun MatchCelebrationDialog(
    matchedItem: TradeItem,
    onDismiss: () -> Unit,
    onProposeTrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onProposeTrade,
                colors = ButtonDefaults.buttonColors(containerColor = PaletteConfig.PrimaryDefault)
            ) {
                Text("Proponer Trueque Ahora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Seguir Explorando")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.SwapCalls,
                contentDescription = null,
                tint = PaletteConfig.SecondaryDefault,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "¡Es un Match de Trueque!",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A ${matchedItem.title} también le interesan tus artículos publicados.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PaletteConfig.PrimaryDefault,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = matchedItem.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Valor est: \$${matchedItem.estimatedValueMin.toInt()} - \$${matchedItem.estimatedValueMax.toInt()} TC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}
