package com.example.estudapp.ui.feature.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.FavoriteLocationDTO
import com.example.estudapp.domain.repository.LocationRepository
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

    // StateFlow para guardar a última localização conhecida do usuário
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
                }.onFailure { error ->
                    _locationsState.value = LocationsUiState.Error(error.message ?: "Erro ao buscar localizações")
                }
            }
        }
    }

    fun createFavoriteLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.saveFavoriteLocation(name, latitude, longitude)
        }
    }

    fun deleteFavoriteLocation(locationId: String) {
        viewModelScope.launch {
            repository.deleteFavoriteLocation(locationId)
            // A UI vai se atualizar sozinha, pois o 'getFavoriteLocations' vai emitir a nova lista
        }
    }

    // Função para buscar a localização atual do dispositivo
    // A anotação SuppressLint é necessária aqui por causa da checagem de permissão do FusedLocationProviderClient
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        viewModelScope.launch {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            // A UI já garante que a permissão foi concedida antes de chamar esta função
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