package com.example.estudapp.ui.feature.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.FavoriteLocationDTO
import com.example.estudapp.domain.receiver.GeofenceBroadcastReceiver
import com.example.estudapp.domain.repository.LocationRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationViewModel : ViewModel() {

    private val repository = LocationRepository()

    private val _locationsState = MutableStateFlow<LocationsUiState>(LocationsUiState.Loading)
    val locationsState: StateFlow<LocationsUiState> = _locationsState.asStateFlow()

    private val _lastKnownLocation = MutableStateFlow<LatLng?>(null)
    val lastKnownLocation: StateFlow<LatLng?> = _lastKnownLocation.asStateFlow()

    init {
        loadFavoriteLocations()
    }

    private fun loadFavoriteLocations() {
        viewModelScope.launch {
            repository.getFavoriteLocations().collect { result ->
                result.onSuccess { locations ->
                    _locationsState.value = LocationsUiState.Success(locations)
                    // A CADA ATUALIZAÇÃO DA LISTA, RE-REGISTRAMOS OS GEOFENCES
                    // O CONTEXTO SERÁ PASSADO PELA UI QUANDO NECESSÁRIO
                }.onFailure { error ->
                    _locationsState.value = LocationsUiState.Error(error.message ?: "Erro ao buscar localizações")
                }
            }
        }
    }

    // --- LÓGICA DE GEOFENCING ---

    // Cria um PendingIntent que aponta para o nosso BroadcastReceiver
    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    // Função principal que cria e registra todos os Geofences
    @SuppressLint("MissingPermission")
    fun registerAllGeofences(context: Context) {
        val geofencingClient = LocationServices.getGeofencingClient(context)

        val currentState = _locationsState.value
        if (currentState !is LocationsUiState.Success || currentState.locations.isEmpty()) {
            Log.d("GeofenceViewModel", "Nenhum local para registrar Geofences.")
            // Se não houver locais, podemos remover quaisquer geofences antigos
            geofencingClient.removeGeofences(getGeofencePendingIntent(context))
            return
        }

        val geofenceList = currentState.locations.map { location ->
            Geofence.Builder()
                .setRequestId(location.name) // Usamos o nome do local como ID
                .setCircularRegion(location.latitude, location.longitude, location.radius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent(context)).run {
            addOnSuccessListener {
                Log.d("GeofenceViewModel", "Geofences adicionados com sucesso!")
            }
            addOnFailureListener { exception ->
                Log.e("GeofenceViewModel", "Falha ao adicionar Geofences: ${exception.message}")
            }
        }
    }

    // --- FUNÇÕES ANTIGAS ---

    fun createFavoriteLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.saveFavoriteLocation(name, latitude, longitude)
        }
    }

    fun deleteFavoriteLocation(locationId: String) {
        viewModelScope.launch {
            repository.deleteFavoriteLocation(locationId)
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        viewModelScope.launch {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    _lastKnownLocation.value = LatLng(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                _locationsState.value = LocationsUiState.Error("Não foi possível obter a localização.")
            }
        }
    }
}

sealed class LocationsUiState {
    object Loading : LocationsUiState()
    data class Success(val locations: List<FavoriteLocationDTO>) : LocationsUiState()
    data class Error(val message: String) : LocationsUiState()
}