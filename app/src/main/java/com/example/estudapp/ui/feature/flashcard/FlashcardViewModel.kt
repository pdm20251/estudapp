package com.example.estudapp.ui.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlashcardViewModel : ViewModel() {

    private val repository = FlashcardRepository()

    // State for the SAVING operation status
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    // State for the LISTING operation UI
    private val _flashcardsState = MutableStateFlow<FlashcardsUiState>(FlashcardsUiState.Loading)
    val flashcardsState: StateFlow<FlashcardsUiState> = _flashcardsState.asStateFlow()

    // This function will now be called by the UI with a specific deckId
    fun loadFlashcards(deckId: String) {
        viewModelScope.launch {
            _flashcardsState.value = FlashcardsUiState.Loading
            repository.getFlashcards(deckId).collect { result ->
                result.onSuccess { flashcards ->
                    _flashcardsState.value = FlashcardsUiState.Success(flashcards)
                }.onFailure { error ->
                    _flashcardsState.value = FlashcardsUiState.Error(error.message ?: "Erro ao carregar flashcards.")
                }
            }
        }
    }

    // --- Save functions updated to require a deckId ---

    fun saveFrenteVerso(deckId: String, frente: String, verso: String) {
        val dto = FlashcardDTO(
            type = FlashcardTypeEnum.FRENTE_VERSO.name,
            frente = frente,
            verso = verso
        )
        save(deckId, dto)
    }

    fun saveCloze(deckId: String, textoComLacunas: String, respostas: Map<String, String>) {
        val dto = FlashcardDTO(
            type = FlashcardTypeEnum.CLOZE.name,
            textoComLacunas = textoComLacunas,
            respostasCloze = respostas
        )
        save(deckId, dto)
    }

    fun saveDigiteResposta(deckId: String, pergunta: String, respostasValidas: List<String>) {
        val dto = FlashcardDTO(
            type = FlashcardTypeEnum.DIGITE_RESPOSTA.name,
            pergunta = pergunta,
            respostasValidas = respostasValidas
        )
        save(deckId, dto)
    }

    fun saveMultiplaEscolha(deckId: String, pergunta: String, alternativas: List<String>, respostaCorreta: String) {
        val dto = FlashcardDTO(
            type = FlashcardTypeEnum.MULTIPLA_ESCOLHA.name,
            pergunta = pergunta,
            alternativas = alternativas,
            respostaCorreta = respostaCorreta
        )
        save(deckId, dto)
    }

    // Private save function now also takes a deckId
    private fun save(deckId: String, flashcard: FlashcardDTO) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            val result = repository.saveFlashcard(deckId, flashcard)
            result.onSuccess {
                _saveStatus.value = SaveStatus.Success("Flashcard salvo!")
            }.onFailure {
                _saveStatus.value = SaveStatus.Error(it.message ?: "Erro ao salvar.")
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }
}

// Sealed class to represent the UI states of the list
sealed class FlashcardsUiState {
    object Loading : FlashcardsUiState()
    data class Success(val flashcards: List<FlashcardDTO>) : FlashcardsUiState()
    data class Error(val message: String) : FlashcardsUiState()
}

// Sealed class for the save status (no changes here)
sealed class SaveStatus {
    object Idle : SaveStatus()
    object Loading : SaveStatus()
    data class Success(val message: String) : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}