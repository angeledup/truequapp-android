package com.truequapp.feature.trade.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.ConditionTag
import com.truequapp.core.designsystem.component.ItemCondition as DesignItemCondition
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.feature.trade.mvi.TradeUiEffect
import com.truequapp.feature.trade.mvi.TradeUiEvent
import com.truequapp.feature.trade.mvi.TradeUiState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTradeProposalScreen(
    state: TradeUiState,
    effectFlow: Flow<TradeUiEffect>,
    onEvent: (TradeUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToNegotiation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            when (effect) {
                is TradeUiEffect.NavigateToNegotiation -> onNavigateToNegotiation(effect.proposalId)
                is TradeUiEffect.NavigateBack -> onNavigateBack()
                is TradeUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proponer Trueque", fontWeight = FontWeight.Bold) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error banner si existe
            state.errorMessage?.let { errorMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Sección 1: Artículo Solicitado
            state.targetItem?.let { target ->
                item {
                    Text(
                        text = "Artículo Solicitado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TargetItemCard(item = target)
                }
            }

            // Sección 2: Selección de tus artículos (Opción A: Al menos 1)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ofrece de tu Catálogo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.selectedOfferedItemIds.size} seleccionados",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.hasMinimumItemsSelected) TruequappTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                if (!state.hasMinimumItemsSelected) {
                    Text(
                        text = "Obligatorio (Opción A): Debes seleccionar al menos un artículo propio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (state.availableUserItems.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No tienes artículos disponibles en tu catálogo para ofrecer. Publica un artículo primero.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(state.availableUserItems, key = { it.id }) { userItem ->
                    SelectableItemRow(
                        item = userItem,
                        isSelected = state.selectedOfferedItemIds.contains(userItem.id),
                        onToggle = { onEvent(TradeUiEvent.ToggleOfferedItemSelection(userItem.id)) }
                    )
                }
            }

            // Sección 3: Compensación en TruequeCoins opcional
            item {
                Text(
                    text = "Diferencial en TruequeCoins (Opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.truequeCoinsInput,
                    onValueChange = { onEvent(TradeUiEvent.OnTruequeCoinsChanged(it)) },
                    label = { Text("Monto de TruequeCoins") },
                    supportingText = {
                        Text("Saldo disponible: ${String.format("%.0f", state.truequeCoins)} TC")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Sección 4: Validador en tiempo real de la Regla RN-02 (Banda de Equivalencia del 40%)
            item {
                EquivalenceRatioBadge(
                    ratio = state.calculatedRatio,
                    isValid = state.isRatioValid,
                    hasMinimumItems = state.hasMinimumItemsSelected
                )
            }

            // Sección 5: Botón de envío
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TruequappPrimaryButton(
                    text = if (state.isSubmitting) "Enviando..." else "Enviar Propuesta Formal",
                    onClick = { onEvent(TradeUiEvent.SubmitProposal) },
                    enabled = state.canSubmitProposal && !state.isSubmitting
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TargetItemCard(item: TradeItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val condition = when (item.condition) {
                    ItemCondition.NUEVO -> DesignItemCondition.NEW
                    ItemCondition.COMO_NUEVO -> DesignItemCondition.LIKE_NEW
                    ItemCondition.USADO -> DesignItemCondition.USED_GOOD
                    ItemCondition.REGULAR -> DesignItemCondition.FAIR
                }
                ConditionTag(condition = condition)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Valor estimado: \$${item.estimatedValue.minValue.toInt()} - \$${item.estimatedValue.maxValue.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TruequappTheme.colorScheme.primary
                )
                Text(
                    text = "Mediana: \$${item.estimatedValue.medianValue.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectableItemRow(
    item: TradeItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                TruequappTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) TruequappTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = TruequappTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Valor estimado: ~\$${item.estimatedValue.medianValue.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EquivalenceRatioBadge(
    ratio: Double,
    isValid: Boolean,
    hasMinimumItems: Boolean
) {
    val percentage = (ratio * 100).coerceAtMost(200.0)
    val color = if (isValid) TruequappTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Regla de Equivalencia RN-02",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Text(
                    text = "${percentage.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (percentage / 100f).toFloat().coerceIn(0f, 1f) },
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isValid) {
                    "¡Oferta equilibrada! Cumple con el mínimo legal del 40% del valor del bien solicitado."
                } else {
                    "La oferta debe representar al menos el 40% del valor para evitar spam o trueques abusivos."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
