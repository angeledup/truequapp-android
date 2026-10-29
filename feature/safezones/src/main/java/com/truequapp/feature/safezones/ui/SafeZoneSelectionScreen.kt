package com.truequapp.feature.safezones.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.SafeZonePoint
import com.truequapp.feature.safezones.mvi.SafeZoneUiEffect
import com.truequapp.feature.safezones.mvi.SafeZoneUiEvent
import com.truequapp.feature.safezones.mvi.SafeZoneUiState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneSelectionScreen(
    tradeId: String,
    currentUserId: String,
    counterpartUserId: String,
    state: SafeZoneUiState,
    effectFlow: Flow<SafeZoneUiEffect>,
    onEvent: (SafeZoneUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToQr: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedCampusFilter by remember { mutableIntStateOf(0) }

    val openGoogleMaps = { zone: SafeZonePoint ->
        val uri = Uri.parse("geo:${zone.coordinates.latitude},${zone.coordinates.longitude}?q=${Uri.encode(zone.name)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${zone.coordinates.latitude},${zone.coordinates.longitude}")
            )
            context.startActivity(fallbackIntent)
        }
    }

    LaunchedEffect(tradeId) {
        onEvent(SafeZoneUiEvent.InitSafeZoneFlow(tradeId, currentUserId, counterpartUserId))
    }

    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            when (effect) {
                is SafeZoneUiEffect.NavigateBack -> onNavigateBack()
                is SafeZoneUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                else -> Unit
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(state.midpointCoordinates.latitude, state.midpointCoordinates.longitude),
            13.5f
        )
    }

    LaunchedEffect(state.selectedSafeZone) {
        state.selectedSafeZone?.let { zone ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(zone.coordinates.latitude, zone.coordinates.longitude),
                    14.5f
                )
            )
        }
    }

    val filteredSafeZones = remember(state.safeZones, selectedCampusFilter) {
        when (selectedCampusFilter) {
            1 -> state.safeZones.filter { it.id.contains("unam") || it.id.contains("universidad") || it.id.contains("delta") }
            2 -> state.safeZones.filter { it.id.contains("uv") }
            else -> state.safeZones
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Puntos Seguros (Safe Zones)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Mapa oficial con API Google Maps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mapa interactivo Google Maps Compose con marcadores de SafeZones (CU019)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = true
                    )
                ) {
                    // Marcador de punto medio / centroide
                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                state.midpointCoordinates.latitude,
                                state.midpointCoordinates.longitude
                            )
                        ),
                        title = "Punto Medio Equidistante",
                        snippet = "Centroide calculado entre ambos usuarios"
                    )

                    // Marcadores de Safe Zones
                    filteredSafeZones.forEach { zone ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    zone.coordinates.latitude,
                                    zone.coordinates.longitude
                                )
                            ),
                            title = zone.name,
                            snippet = "Horario: ${zone.openingHour}:00 - ${zone.closingHour}:00",
                            onClick = {
                                onEvent(SafeZoneUiEvent.SelectSafeZone(zone.id))
                                true
                            }
                        )
                    }
                }

                // Badge flotante indicativo
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = TruequappTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Monitoreo 24/7 y CCTV Activo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Filtros de Región / Campus
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCampusFilter == 0,
                    onClick = { selectedCampusFilter = 0 },
                    label = { Text("Todos los Puntos (${state.safeZones.size})") }
                )
                FilterChip(
                    selected = selectedCampusFilter == 1,
                    onClick = { selectedCampusFilter = 1 },
                    label = { Text("UNAM (CDMX)") }
                )
                FilterChip(
                    selected = selectedCampusFilter == 2,
                    onClick = { selectedCampusFilter = 2 },
                    label = { Text("UV (Veracruz / Xalapa)") }
                )
            }

            // Lista de Puntos Seguros Verificados
            Text(
                text = "Puntos de Encuentro Disponibles (${filteredSafeZones.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSafeZones, key = { it.id }) { zone ->
                    val isSelected = zone.id == state.selectedSafeZone?.id
                    SafeZoneCard(
                        zone = zone,
                        isSelected = isSelected,
                        onSelect = { onEvent(SafeZoneUiEvent.SelectSafeZone(zone.id)) },
                        onNavigateWithGps = { openGoogleMaps(zone) }
                    )
                }
            }

            // Barra inferior con botón para agendar y proceder al QR
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TruequappPrimaryButton(
                        text = if (state.appointment != null) "Ver Código QR de Entrega" else "Confirmar Cita en Este Punto",
                        onClick = {
                            if (state.appointment != null) {
                                onNavigateToQr(tradeId)
                            } else {
                                onEvent(SafeZoneUiEvent.ConfirmAppointment)
                            }
                        },
                        enabled = state.selectedSafeZone != null
                    )
                }
            }
        }
    }
}

@Composable
private fun SafeZoneCard(
    zone: SafeZonePoint,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onNavigateWithGps: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                TruequappTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) TruequappTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = zone.addressDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${zone.openingHour}:00 - ${zone.closingHour}:00",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (zone.hasVideoSurveillance) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(12.dp), tint = TruequappTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CCTV 24/7", fontSize = 11.sp, color = TruequappTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onNavigateWithGps,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cómo llegar con GPS", fontSize = 11.sp)
                }
            }
        }
    }
}


