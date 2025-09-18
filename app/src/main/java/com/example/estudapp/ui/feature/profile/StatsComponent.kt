package com.example.estudapp.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.DeckPlayStatDTO
import com.example.estudapp.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DeckStatistics(
    val deckId: String,
    val deckName: String,
    val totalScore: Double,
    val totalPossible: Double,
    val percentage: Double,
    val sessionCount: Int
)

data class LocationStatistics(
    val locationId: String,
    val locationName: String,
    val totalScore: Double,
    val totalPossible: Double,
    val percentage: Double,
    val sessionCount: Int
)

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(
        val deckStats: List<DeckStatistics>,
        val locationStats: List<LocationStatistics>
    ) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

class StatsViewModel : ViewModel() {

    private val repository = FlashcardRepository()

    private val _statsState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val statsState: StateFlow<StatsUiState> = _statsState.asStateFlow()

    private val _isLocationView = MutableStateFlow(false)
    val isLocationView: StateFlow<Boolean> = _isLocationView.asStateFlow()

    init {
        loadStatistics()
    }

    fun toggleView() {
        _isLocationView.value = !_isLocationView.value
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _statsState.value = StatsUiState.Loading

            try {
                // Busca todas as estatísticas do usuário
                val allStatsResult = repository.getAllUserStats()
                if (allStatsResult.isFailure) {
                    _statsState.value = StatsUiState.Error("Erro ao carregar estatísticas")
                    return@launch
                }

                val allStats = allStatsResult.getOrNull() ?: emptyList()

                // Busca localizações favoritas
                val locationsResult = repository.getUserFavoriteLocations()
                val favoriteLocations = locationsResult.getOrNull() ?: emptyList()

                // Processa estatísticas por deck
                val deckStatsMap = mutableMapOf<String, MutableList<DeckPlayStatDTO>>()
                allStats.forEach { stat ->
                    val deckId = stat.deckId ?: return@forEach
                    deckStatsMap.getOrPut(deckId) { mutableListOf() }.add(stat)
                }

                val deckStatistics = deckStatsMap.map { (deckId, sessions) ->
                    val totalScore = sessions.sumOf { it.totalScore ?: 0.0 }
                    val totalPossible = sessions.sumOf { it.totalPossible ?: 0.0 }
                    val percentage = if (totalPossible > 0) (totalScore / totalPossible * 100) else 0.0

                    // Busca nome do deck
                    val deckInfo = repository.getDeckInfo(deckId).getOrNull()
                    val deckName = deckInfo?.name ?: "Deck $deckId"

                    DeckStatistics(
                        deckId = deckId,
                        deckName = deckName,
                        totalScore = totalScore,
                        totalPossible = totalPossible,
                        percentage = percentage.roundToInt().toDouble(),
                        sessionCount = sessions.size
                    )
                }.sortedByDescending { it.percentage }

                // Processa estatísticas por localização
                val locationStatsMap = mutableMapOf<String, MutableList<DeckPlayStatDTO>>()

                allStats.forEach { stat ->
                    val statLat = stat.latitude
                    val statLon = stat.longitude

                    if (statLat != null && statLon != null) {
                        // Encontra a localização favorita mais próxima
                        val nearestLocation = favoriteLocations.minByOrNull { location ->
                            repository.calculateDistance(
                                statLat, statLon,
                                location.latitude, location.longitude
                            )
                        }

                        // Verifica se está dentro do raio
                        nearestLocation?.let { location ->
                            val distance = repository.calculateDistance(
                                statLat, statLon,
                                location.latitude, location.longitude
                            )

                            if (distance <= location.radius) {
                                locationStatsMap.getOrPut(location.id) { mutableListOf() }.add(stat)
                            }
                        }
                    }
                }

                val locationStatistics = favoriteLocations.map { location ->
                    val sessions = locationStatsMap[location.id] ?: emptyList()
                    val totalScore = sessions.sumOf { it.totalScore ?: 0.0 }
                    val totalPossible = sessions.sumOf { it.totalPossible ?: 0.0 }
                    val percentage = if (totalPossible > 0) (totalScore / totalPossible * 100) else 0.0

                    LocationStatistics(
                        locationId = location.id,
                        locationName = location.name,
                        totalScore = totalScore,
                        totalPossible = totalPossible,
                        percentage = percentage.roundToInt().toDouble(),
                        sessionCount = sessions.size
                    )
                }.filter { it.sessionCount > 0 }.sortedByDescending { it.percentage }

                _statsState.value = StatsUiState.Success(
                    deckStats = deckStatistics,
                    locationStats = locationStatistics
                )

            } catch (e: Exception) {
                _statsState.value = StatsUiState.Error("Erro ao processar estatísticas: ${e.message}")
            }
        }
    }
}