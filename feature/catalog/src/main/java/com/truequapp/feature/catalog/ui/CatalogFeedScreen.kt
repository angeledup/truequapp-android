package com.truequapp.feature.catalog.ui

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.TruequappTopAppBar
import com.truequapp.core.domain.model.CategoryTaxonomy
import com.truequapp.feature.catalog.mvi.CatalogUiEffect
import com.truequapp.feature.catalog.mvi.CatalogUiEvent
import com.truequapp.feature.catalog.mvi.CatalogUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Pantalla de Exploración del Marketplace con Feed en cuadrícula y filtros PostGIS geolocalizados (CU008).
 */
@Composable
fun CatalogFeedScreen(
    state: CatalogUiState,
    effectFlow: Flow<CatalogUiEffect>,
    onEvent: (CatalogUiEvent) -> Unit,
    onNavigateToItemDetail: (String) -> Unit,
    onNavigateToPublish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDistanceFilter by remember { mutableStateOf(false) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TruequappTopAppBar(
                title = "Marketplace Truequapp",
                coinsBalance = 150
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToPublish,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Publicar Artículo")
            }
        },
        modifier = modifier.fillMaxSize()
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
                        IconButton(onClick = { showDistanceFilter = !showDistanceFilter }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filtros",
                                tint = if (showDistanceFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            )

            // Filtro de Distancia Geográfica ($1\text{ km}$ a $50\text{ km}$) según CU008
            if (showDistanceFilter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
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
                                text = "Radio Máximo de Proximidad:",
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
                }
            }

            // Chips horizontales de categorías
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedCategoryId == null,
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

            // Cuadrícula de Artículos (LazyVerticalGrid)
            if (state.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron artículos con los filtros seleccionados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onClick = { onNavigateToItemDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}

