package com.example.estudapp.ui.feature.location

// --- IMPORTS QUE ESTAVAM FALTANDO ---
import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudapp.data.model.FavoriteLocationDTO // <-- Importante
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory // <-- Importante
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    locationViewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }

    if (locationPermissionState.status.isGranted) {
        LaunchedEffect(Unit) {
            locationViewModel.getCurrentLocation(context)
        }
        MapContent(locationViewModel)
    } else {
        PermissionDeniedContent {
            locationPermissionState.launchPermissionRequest()
        }
    }
}

@Composable
fun MapContent(locationViewModel: LocationViewModel) {
    val locationsState by locationViewModel.locationsState.collectAsState()
    val lastKnownLocation by locationViewModel.lastKnownLocation.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<FavoriteLocationDTO?>(null) }
    var locationName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val atLimit = locationsState is LocationsUiState.Success &&
            (locationsState as LocationsUiState.Success).locations.size >= 7

    val defaultLocation = LatLng(-18.9186, -48.2772) // Uberlândia
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lastKnownLocation ?: defaultLocation, 15f)
    }

    LaunchedEffect(lastKnownLocation) {
        lastKnownLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(it, 15f)
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 2. A ação de clique agora verifica o limite primeiro.
                    if (atLimit) {
                        Toast.makeText(context, "Limite de 7 locais atingido!", Toast.LENGTH_SHORT).show()
                    } else if (lastKnownLocation != null) {
                        showAddDialog = true
                    } else {
                        Toast.makeText(context, "Aguardando sua localização...", Toast.LENGTH_SHORT).show()
                    }
                },
                // 3. Mudamos a cor do botão para indicar que está "desabilitado".
                containerColor = if (atLimit) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                else FloatingActionButtonDefaults.containerColor
            ) {
                Text("  Adicionar Local Atual  ", modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(padding),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            lastKnownLocation?.let {
                Marker(
                    state = rememberMarkerState(position = it),
                    title = "Sua Localização"
                )
            }

            if (locationsState is LocationsUiState.Success) {
                (locationsState as LocationsUiState.Success).locations.forEach { location ->
                    Marker(
                        state = rememberMarkerState(position = LatLng(location.latitude, location.longitude)),
                        title = location.name,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        onClick = {
                            selectedLocation = location
                            true // Evento consumido
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar Local Favorito") },
            text = {
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Nome do Local (Ex: Casa)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        lastKnownLocation?.let {
                            locationViewModel.createFavoriteLocation(locationName, it.latitude, it.longitude)
                            Toast.makeText(context, "'$locationName' salvo!", Toast.LENGTH_SHORT).show()
                        }
                        locationName = ""
                        showAddDialog = false
                    },
                    enabled = locationName.isNotBlank()
                ) { Text("Salvar") }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }

    selectedLocation?.let { locationToDelete ->
        AlertDialog(
            onDismissRequest = { selectedLocation = null },
            title = { Text("Excluir Local") },
            text = { Text("Tem certeza que deseja excluir o local '${locationToDelete.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        locationViewModel.deleteFavoriteLocation(locationToDelete.id)
                        Toast.makeText(context, "'${locationToDelete.name}' excluído!", Toast.LENGTH_SHORT).show()
                        selectedLocation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                Button(onClick = { selectedLocation = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("A permissão de localização é essencial para esta funcionalidade.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Conceder Permissão")
        }
    }
}