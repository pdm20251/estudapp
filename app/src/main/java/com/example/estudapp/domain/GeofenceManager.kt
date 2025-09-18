package com.example.estudapp.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton para gerenciar e compartilhar o estado do Geofence atual
 * entre o BroadcastReceiver e a UI.
 */
object GeofenceManager {
    private val _currentLocation = MutableStateFlow<String?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    fun updateCurrentLocation(locationName: String?) {
        _currentLocation.value = locationName
    }
}