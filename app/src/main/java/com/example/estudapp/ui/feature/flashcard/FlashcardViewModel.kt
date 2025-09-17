package com.example.estudapp.ui.feature.flashcard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.AlternativaDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.domain.repository.FlashcardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlashcardViewModel : ViewModel() {

    private val repository = FlashcardRepository()

    // --- Estados da UI ---
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _flashcardsState = MutableStateFlow<FlashcardsUiState>(FlashcardsUiState.Loading)
    val flashcardsState: StateFlow<FlashcardsUiState> = _flashcardsState.asStateFlow()

    private val _cardToEdit = MutableStateFlow<FlashcardDTO?>(null)
    val cardToEdit: StateFlow<FlashcardDTO?> = _cardToEdit.asStateFlow()

    // --- Funções de Carregamento ---
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

    fun loadFlashcardForEditing(deckId: String, flashcardId: String) {
        viewModelScope.launch {
            repository.getFlashcard(deckId, flashcardId).onSuccess {
                _cardToEdit.value = it
            }
        }
    }

    fun clearCardToEdit() {
        _cardToEdit.value = null
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    // --- Funções de SALVAR ---
    private fun save(deckId: String, flashcard: FlashcardDTO) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            repository.saveFlashcard(deckId, flashcard).onSuccess {
                _saveStatus.value = SaveStatus.Success("Flashcard salvo!")
            }.onFailure {
                _saveStatus.value = SaveStatus.Error(it.message ?: "Erro ao salvar.")
            }
        }
    }

    fun saveFrenteVerso(deckId: String, frente: String, verso: String, imagemUri: Uri?, audioUri: Uri?) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            val imageUrl = if (imagemUri != null) repository.uploadFile(imagemUri).getOrNull() else null
            val audioUrl = if (audioUri != null) repository.uploadFile(audioUri).getOrNull() else null
            val dto = FlashcardDTO(type = FlashcardTypeEnum.FRENTE_VERSO.name, frente = frente, verso = verso, perguntaImageUrl = imageUrl, perguntaAudioUrl = audioUrl)
            save(deckId, dto)
        }
    }

    fun saveCloze(deckId: String, texto: String, respostasMap: Map<String, String>) {
        val dto = FlashcardDTO(type = FlashcardTypeEnum.CLOZE.name, textoComLacunas = texto, respostasCloze = respostasMap)
        save(deckId, dto)
    }

    fun saveDigiteResposta(deckId: String, pergunta: String, respostasList: List<String>, imagemUri: Uri?, audioUri: Uri?) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            val imageUrl = if (imagemUri != null) repository.uploadFile(imagemUri).getOrNull() else null
            val audioUrl = if (audioUri != null) repository.uploadFile(audioUri).getOrNull() else null
            val dto = FlashcardDTO(type = FlashcardTypeEnum.DIGITE_RESPOSTA.name, pergunta = pergunta, respostasValidas = respostasList, perguntaImageUrl = imageUrl, perguntaAudioUrl = audioUrl)
            save(deckId, dto)
        }
    }

    fun saveMultiplaEscolha(deckId: String, pergunta: String, perguntaImagemUri: Uri?, perguntaAudioUri: Uri?, alternativasUris: List<Pair<String, Uri?>>, respostaCorretaIndex: Int) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            try {
                val perguntaImageDeferred = async { if (perguntaImagemUri != null) repository.uploadFile(perguntaImagemUri) else null }
                val perguntaAudioDeferred = async { if (perguntaAudioUri != null) repository.uploadFile(perguntaAudioUri) else null }
                val alternativasDeferred = alternativasUris.map { (_, uri) -> async { if (uri != null) repository.uploadFile(uri) else null } }

                val perguntaImageUrl = perguntaImageDeferred.await()?.getOrNull()
                val perguntaAudioUrl = perguntaAudioDeferred.await()?.getOrNull()
                val alternativasUrls = alternativasDeferred.awaitAll().map { it?.getOrNull() }

                val alternativas = alternativasUris.mapIndexed { index, (texto, _) -> AlternativaDTO(text = texto.ifBlank { null }, imageUrl = alternativasUrls[index]) }
                val dto = FlashcardDTO(type = FlashcardTypeEnum.MULTIPLA_ESCOLHA.name, pergunta = pergunta, perguntaImageUrl = perguntaImageUrl, perguntaAudioUrl = perguntaAudioUrl, alternativas = alternativas, respostaCorreta = alternativas[respostaCorretaIndex])
                save(deckId, dto)
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error("Falha no upload: ${e.message}")
            }
        }
    }

    // --- Funções de ATUALIZAR ---
    private fun updateFlashcard(deckId: String, flashcard: FlashcardDTO) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            repository.updateFlashcard(deckId, flashcard).onSuccess {
                _saveStatus.value = SaveStatus.Success("Flashcard atualizado!")
            }.onFailure {
                _saveStatus.value = SaveStatus.Error(it.message ?: "Erro ao atualizar.")
            }
        }
    }

    fun updateFrenteVerso(deckId: String, originalCard: FlashcardDTO, frente: String, verso: String) {
        val updatedCard = originalCard.copy(frente = frente, verso = verso)
        updateFlashcard(deckId, updatedCard)
    }

    fun updateCloze(deckId: String, originalCard: FlashcardDTO, texto: String, respostasMap: Map<String, String>) {
        val updatedCard = originalCard.copy(textoComLacunas = texto, respostasCloze = respostasMap)
        updateFlashcard(deckId, updatedCard)
    }

    fun updateDigiteResposta(deckId: String, originalCard: FlashcardDTO, pergunta: String, respostasList: List<String>) {
        val updatedCard = originalCard.copy(pergunta = pergunta, respostasValidas = respostasList)
        updateFlashcard(deckId, updatedCard)
    }

    fun updateMultiplaEscolha(deckId: String, originalCard: FlashcardDTO, pergunta: String, alternativas: List<AlternativaDTO>, respostaCorretaIndex: Int) {
        val updatedCard = originalCard.copy(
            pergunta = pergunta,
            alternativas = alternativas,
            respostaCorreta = alternativas.getOrNull(respostaCorretaIndex)
        )
        updateFlashcard(deckId, updatedCard)
    }

    fun deleteFlashcard(deckId: String, flashcardId: String) {
        viewModelScope.launch {
            repository.deleteFlashcard(deckId, flashcardId)
        }
    }
}

// --- Sealed Classes (sem alterações) ---
sealed class FlashcardsUiState {
    object Loading : FlashcardsUiState()
    data class Success(val flashcards: List<FlashcardDTO>) : FlashcardsUiState()
    data class Error(val message: String) : FlashcardsUiState()
}

sealed class SaveStatus {
    object Idle : SaveStatus()
    object Loading : SaveStatus()
    data class Success(val message: String) : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}