package com.example.estudapp.ui.feature.location

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.navigation.NavHostController

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

    var showDialog by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }

    val defaultLocation = LatLng(-18.9186, -48.2772) // Uberlândia
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lastKnownLocation ?: defaultLocation, 15f)
    }

    LaunchedEffect(lastKnownLocation) {
        lastKnownLocation?.let {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(it, 15f) // <-- CORRIGIDO
            )

        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (lastKnownLocation != null) {
                    showDialog = true
                }
            }) {
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
                        state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                        title = location.name,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                        }
                        locationName = ""
                        showDialog = false
                    },
                    enabled = locationName.isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
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