package com.truequapp.feature.catalog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.TruequappTopAppBar
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.CategoryTaxonomy
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.feature.catalog.mvi.CatalogUiEffect
import com.truequapp.feature.catalog.mvi.CatalogUiEvent
import com.truequapp.feature.catalog.mvi.CatalogUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Pantalla de Exploración del Marketplace con Feed en cuadrícula (LazyVerticalGrid), paginación incremental,
 * filtros geolocalizados, filtro por condición y rango de valor estimado (CU008 / Fase 2).
 */
@Composable
fun CatalogFeedScreen(
    state: CatalogUiState,
    effectFlow: Flow<CatalogUiEffect>,
    onEvent: (CatalogUiEvent) -> Unit,
    onNavigateToItemDetail: (String) -> Unit,
    onNavigateToPublish: () -> Unit,
    onCoinsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdvancedFilters by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    // Detección de fin de lista para paginación incremental (Paging)
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 2 && state.hasMorePages && !state.isPaginating
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onEvent(CatalogUiEvent.LoadNextPage)
        }
    }

    LaunchedEffect(effectFlow) {
        effectFlow.collectLatest { effect ->
            when (effect) {
                is CatalogUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is CatalogUiEffect.NavigateToItemDetail -> onNavigateToItemDetail(effect.itemId)
                is CatalogUiEffect.ItemPublishedSuccess -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TruequappTopAppBar(
                title = "Explorar Trueques",
                coinsBalance = state.truequeCoins.toInt(),
                unreadNotifications = 0,
                onCoinsClick = onCoinsClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToPublish,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Publicar artículo")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barra de Búsqueda
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onEvent(CatalogUiEvent.SearchQueryChanged(it)) },
                placeholder = { Text("Buscar artículos o deseos en wishlist...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onEvent(CatalogUiEvent.SearchQueryChanged("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                        IconButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filtros",
                                tint = if (showAdvancedFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium
            )

            // Panel Desplegable de Filtros Avanzados (Distancia, Condición y Rango de Valor)
            AnimatedVisibility(visible = showAdvancedFilters) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 1. Radio de Proximidad en Coatzacoalcos (1 km a 50 km)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Radio de Proximidad:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${state.maxDistanceKm.toInt()} km",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = state.maxDistanceKm,
                            onValueChange = { onEvent(CatalogUiEvent.DistanceFilterChanged(it)) },
                            valueRange = 1f..50f,
                            steps = 49,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Filtro de Condición Física
                        Text(
                            text = "Condición Física:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedCondition == null,
                                onClick = { onEvent(CatalogUiEvent.ConditionFilterSelected(null)) },
                                label = { Text("Todas") }
                            )
                            ItemCondition.entries.forEach { condition ->
                                FilterChip(
                                    selected = state.selectedCondition == condition,
                                    onClick = { onEvent(CatalogUiEvent.ConditionFilterSelected(condition)) },
                                    label = { Text(condition.name.replace("_", " ")) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Filtro por Rango de Valor Estimado (MXN)
                        Text(
                            text = "Rango de Valor Estimado (MXN):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.filterMinEstimatedValue?.toInt()?.toString().orEmpty(),
                                onValueChange = {
                                    val min = it.toDoubleOrNull()
                                    onEvent(CatalogUiEvent.ValueFilterChanged(min, state.filterMaxEstimatedValue))
                                },
                                placeholder = { Text("Mín $") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.filterMaxEstimatedValue?.toInt()?.toString().orEmpty(),
                                onValueChange = {
                                    val max = it.toDoubleOrNull()
                                    onEvent(CatalogUiEvent.ValueFilterChanged(state.filterMinEstimatedValue, max))
                                },
                                placeholder = { Text("Máx $") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Chips horizontales: Filtros especiales (Guardados / Mis Publicaciones) y categorías
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filtro de Guardados / Favoritos
                FilterChip(
                    selected = state.filterOnlyBookmarked,
                    onClick = { onEvent(CatalogUiEvent.ToggleBookmarkedFilter) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Guardados (${state.bookmarkedItemIds.size})") }
                )

                // Filtro de Mis Publicaciones
                FilterChip(
                    selected = state.filterOnlyMyPublications,
                    onClick = { onEvent(CatalogUiEvent.ToggleMyPublicationsFilter) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Mis Publicaciones (${state.myInventory.size})") }
                )

                FilterChip(
                    selected = state.selectedCategoryId == null && !state.filterOnlyBookmarked && !state.filterOnlyMyPublications,
                    onClick = { onEvent(CatalogUiEvent.CategoryFilterSelected(null)) },
                    label = { Text("Todas") }
                )

                CategoryTaxonomy.LIQUID_CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = { onEvent(CatalogUiEvent.CategoryFilterSelected(category.id)) },
                        label = { Text(category.name.take(20)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cuadrícula Paginada de Artículos (LazyVerticalGrid)
            val displayItems = state.paginatedItems.ifEmpty { state.filteredItems }
            if (displayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            state.filterOnlyBookmarked -> "No tienes publicaciones guardadas activas en este momento."
                            state.filterOnlyMyPublications -> "Aún no has publicado ningún artículo. Presiona '+' para publicar."
                            else -> "No se encontraron artículos con los filtros seleccionados."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onClick = { onNavigateToItemDetail(item.id) },
                            isBookmarked = state.bookmarkedItemIds.contains(item.id),
                            onToggleBookmark = { onEvent(CatalogUiEvent.ToggleBookmark(item.id)) }
                        )
                    }

                    // Elemento inferior de paginación
                    if (state.hasMorePages) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isPaginating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = TruequappTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "Desliza para cargar más artículos...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
