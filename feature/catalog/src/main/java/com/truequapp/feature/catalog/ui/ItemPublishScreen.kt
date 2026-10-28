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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.component.TruequappTopAppBar
import com.truequapp.core.domain.model.CategoryTaxonomy
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.feature.catalog.mvi.CatalogUiEvent
import com.truequapp.feature.catalog.mvi.CatalogUiState

/**
 * Pantalla de Registro y Publicación de Artículo con Compresión WebP nativa (CU005).
 * Integra PhotoPicker nativo sin permisos intrusivos y valida estrictamente la regla RN-11.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemPublishScreen(
    state: CatalogUiState,
    onEvent: (CatalogUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tagInput by remember { mutableStateOf("") }

    // PhotoPicker nativo de Android (máximo 6 fotos según especificación CU005)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris: List<Uri> ->
        val uriStrings = uris.map { it.toString() }
        onEvent(CatalogUiEvent.ImagesSelected(uriStrings))
    }

    Scaffold(
        topBar = {
            TruequappTopAppBar(
                title = "Publicar Artículo en Truequapp",
                coinsBalance = 100
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
                text = "Fotografías del Artículo (Máx. 6)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Se convertirán automáticamente a WebP (< 80 KB) con eliminación de metadatos GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fila de fotos seleccionadas + botón de agregar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Seleccionar fotos",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.selectedImageUris.size}/6",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                items(state.selectedImageUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título
            OutlinedTextField(
                value = state.newTitle,
                onValueChange = { onEvent(CatalogUiEvent.TitleChanged(it)) },
                label = { Text("Título del Artículo") },
                placeholder = { Text("Ej. Laptop Dell Inspiron 15") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Descripción
            OutlinedTextField(
                value = state.newDescription,
                onValueChange = { onEvent(CatalogUiEvent.DescriptionChanged(it)) },
                label = { Text("Descripción Detallada") },
                placeholder = { Text("Describe detalles de uso, accesorios incluidos y funcionamiento...") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categoría
            Text(
                text = "Categoría Principal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CategoryTaxonomy.LIQUID_CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = state.newCategoryId == category.id,
                        onClick = { onEvent(CatalogUiEvent.CategoryChanged(category.id)) },
                        label = { Text(category.name, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Condición Física
            Text(
                text = "Estado de Conservación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ItemCondition.entries.forEach { condition ->
                    FilterChip(
                        selected = state.newCondition == condition,
                        onClick = { onEvent(CatalogUiEvent.ConditionChanged(condition)) },
                        label = { Text(condition.displayName.take(12), fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rango de Valor Estimado (RN-02)
            Text(
                text = "Rango de Valor Estimado (\$ MXN)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Usado para verificar automáticamente la regla RN-02 (ofertas $\\ge$ 40%).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.newEstimatedValueMin.toInt().toString(),
                    onValueChange = {
                        val min = it.toDoubleOrNull() ?: 0.0
                        onEvent(CatalogUiEvent.ValueRangeChanged(min, state.newEstimatedValueMax))
                    },
                    label = { Text("Mínimo (\$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = state.newEstimatedValueMax.toInt().toString(),
                    onValueChange = {
                        val max = it.toDoubleOrNull() ?: 0.0
                        onEvent(CatalogUiEvent.ValueRangeChanged(state.newEstimatedValueMin, max))
                    },
                    label = { Text("Máximo (\$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Etiquetas Wishlist ("Qué busco a cambio")
            Text(
                text = "Wishlist: ¿Qué te gustaría recibir a cambio?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("Ej. Guitarra, Bicicleta R29...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            onEvent(CatalogUiEvent.AddWishlistTag(tagInput))
                            tagInput = ""
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar etiqueta")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.newWishlistTags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onEvent(CatalogUiEvent.RemoveWishlistTag(tag)) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Mensaje de Error (RN-11 o validación)
            if (!state.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TruequappPrimaryButton(
                text = if (state.isPublishing) "Comprimiendo y Publicando..." else "Publicar en Marketplace",
                onClick = { onEvent(CatalogUiEvent.SubmitPublish) },
                enabled = !state.isPublishing && state.newTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

