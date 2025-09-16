package com.example.estudapp.ui.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.DeckDTO
import com.example.estudapp.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel para gerenciar a lógica dos Decks
class DeckViewModel : ViewModel() {

    private val repository = FlashcardRepository()

    private val _decksState = MutableStateFlow<DecksUiState>(DecksUiState.Loading)
    val decksState: StateFlow<DecksUiState> = _decksState.asStateFlow()

    init {
        loadDecks()
    }

    private fun loadDecks() {
        viewModelScope.launch {
            repository.getDecks().collect { result ->
                result.onSuccess { decks ->
                    _decksState.value = DecksUiState.Success(decks)
                }.onFailure { error ->
                    _decksState.value = DecksUiState.Error(error.message ?: "Erro")
                }
            }
        }
    }

    fun createDeck(name: String, description: String) {
        viewModelScope.launch {
            repository.saveDeck(name, description)
            // A lista irá se atualizar automaticamente por causa do listener em tempo real
        }
    }
}

sealed class DecksUiState {
    object Loading : DecksUiState()
    data class Success(val decks: List<DeckDTO>) : DecksUiState()
    data class Error(val message: String) : DecksUiState()
}