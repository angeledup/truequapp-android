package com.truequapp.feature.catalog.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.CategoryTaxonomy
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.feature.catalog.mvi.CatalogUiEvent
import com.truequapp.feature.catalog.mvi.CatalogUiState

/**
 * Pantalla de Registro y Publicación de Artículo con Compresión WebP nativa (< 80 KB).
 * Restringe la publicación a un máximo estricto de 1 sola fotografía y valida la regla RN-11.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemPublishScreen(
    state: CatalogUiState,
    onEvent: (CatalogUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tagInput by remember { mutableStateOf("") }

    // PhotoPicker nativo de Android: Estrictamente 1 sola fotografía
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        onEvent(CatalogUiEvent.ImageSelected(uri?.toString()))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicar Artículo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Fotografía del Artículo (1 Imagen Máximo)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Se convertirá automáticamente a formato WebP (< 80 KB) con eliminación total de metadatos GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta de foto única: subir o previsualizar
            if (state.selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, TruequappTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = state.selectedImageUri,
                        contentDescription = "Foto del artículo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Botón para eliminar o cambiar foto
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { onEvent(CatalogUiEvent.ImageSelected(null)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remover fotografía",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Badge de compresión WebP
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(TruequappTheme.colorScheme.primary.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "WebP Lossy (< 80 KB) • GPS purgado",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Seleccionar fotografía",
                            tint = TruequappTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Seleccionar 1 Imagen",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TruequappTheme.colorScheme.primary
                        )
                        Text(
                            text = "Toca para abrir la galería de fotos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Datos del Artículo
            Text(
                text = "Detalles de la Publicación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.newTitle,
                onValueChange = { onEvent(CatalogUiEvent.TitleChanged(it)) },
                label = { Text("Título del bien o servicio") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.newDescription,
                onValueChange = { onEvent(CatalogUiEvent.DescriptionChanged(it)) },
                label = { Text("Descripción detallada del estado") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categoría
            Text(
                text = "Categoría Oficial de Alta Liquidez",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CategoryTaxonomy.LIQUID_CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = state.newCategoryId == category.id,
                        onClick = { onEvent(CatalogUiEvent.CategoryChanged(category.id)) },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Condición física
            Text(text = "Condición Física", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemCondition.entries.forEach { condition ->
                    FilterChip(
                        selected = state.newCondition == condition,
                        onClick = { onEvent(CatalogUiEvent.ConditionChanged(condition)) },
                        label = { Text(condition.name.replace("_", " ")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rango de Valor Estimado (RN-02)
            Text(
                text = "Banda de Valor Estimado (MXN)",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Se utiliza para verificar la banda de equivalencia mínima del 40% (RN-02).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.newEstimatedValueMin.toInt().toString(),
                    onValueChange = {
                        val min = it.toDoubleOrNull() ?: 0.0
                        onEvent(CatalogUiEvent.ValueRangeChanged(min, state.newEstimatedValueMax))
                    },
                    label = { Text("Mínimo ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.newEstimatedValueMax.toInt().toString(),
                    onValueChange = {
                        val max = it.toDoubleOrNull() ?: 0.0
                        onEvent(CatalogUiEvent.ValueRangeChanged(state.newEstimatedValueMin, max))
                    },
                    label = { Text("Máximo ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wishlist Tags
            Text(
                text = "¿Qué buscas a cambio? (Wishlist Tags)",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("Ej. Nintendo Switch, Libros...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (tagInput.isNotBlank()) {
                        onEvent(CatalogUiEvent.AddWishlistTag(tagInput))
                        tagInput = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar tag")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.newWishlistTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onEvent(CatalogUiEvent.RemoveWishlistTag(tag)) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Error si lo hay
            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isPublishing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TruequappTheme.colorScheme.primary)
                }
            } else {
                TruequappPrimaryButton(
                    text = "Publicar y Guardar Artículo",
                    onClick = { onEvent(CatalogUiEvent.SubmitPublication) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
