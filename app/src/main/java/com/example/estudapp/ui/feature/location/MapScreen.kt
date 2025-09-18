package com.example.estudapp.ui.feature.location

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudapp.data.model.FavoriteLocationDTO
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    locationViewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current

    val foregroundLocationPermission = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        null
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    LaunchedEffect(notificationPermission) {
        notificationPermission?.launchPermissionRequest()
    }

    LaunchedEffect(Unit) {
        foregroundLocationPermission.launchPermissionRequest()
    }

    if (foregroundLocationPermission.status.isGranted) {
        LaunchedEffect(Unit) {
            locationViewModel.getCurrentLocation(context)
        }
        MapContent(locationViewModel)

        backgroundLocationPermission?.let {
            if (it.status.isGranted) {
                // --- CORREÇÃO IMPORTANTE AQUI ---
                // Agora que temos todas as permissões, observamos a lista de locais.
                // Assim que a lista for carregada com sucesso, registamos os Geofences.
                val locationsState by locationViewModel.locationsState.collectAsState()
                if (locationsState is LocationsUiState.Success) {
                    // Este LaunchedEffect será executado novamente se a lista de locais mudar (adicionar/deletar),
                    // mantendo os geofences sempre sincronizados.
                    LaunchedEffect(locationsState) {
                        locationViewModel.registerAllGeofences(context)
                    }
                }
            } else {
                // Se não tiver a permissão de background, pedimos.
                BackgroundPermissionDialog {
                    it.launchPermissionRequest()
                }
            }
        }
    } else {
        PermissionDeniedContent {
            foregroundLocationPermission.launchPermissionRequest()
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

    val defaultLocation = LatLng(-18.9186, -48.2772)
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
                    if (atLimit) {
                        Toast.makeText(context, "Limite de 7 locais atingido!", Toast.LENGTH_SHORT).show()
                    } else if (lastKnownLocation != null) {
                        showAddDialog = true
                    } else {
                        Toast.makeText(context, "Aguardando sua localização...", Toast.LENGTH_SHORT).show()
                    }
                },
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
                            true
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
                Button(onClick = {
                    locationName = ""
                    showAddDialog = false
                }) { Text("Cancelar") }
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

@Composable
fun BackgroundPermissionDialog(onPermissionRequest: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Permissão Adicional Necessária") },
            text = { Text("Para que o app possa te notificar sobre locais de estudo mesmo quando estiver fechado, por favor, selecione a opção 'Permitir o tempo todo' na próxima tela de permissão.") },
            confirmButton = {
                Button(onClick = {
                    onPermissionRequest()
                    showDialog = false
                }) {
                    Text("Entendi, continuar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Agora não")
                }
            }
        )
    }
}