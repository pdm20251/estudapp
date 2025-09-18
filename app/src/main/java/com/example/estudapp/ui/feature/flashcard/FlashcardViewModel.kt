package com.example.estudapp.ui.feature.flashcard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.AlternativaDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.domain.repository.FlashcardRepository
import com.example.estudapp.domain.stats.DeckSessionManager
import com.example.estudapp.tools.HardcodedStats
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.google.android.gms.tasks.Tasks
import java.net.HttpURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import org.json.JSONObject
import java.io.OutputStreamWriter

data class ValidationResponse(val isCorrect: Boolean, val score: Double)
class FlashcardViewModel : ViewModel() {

    private val repository = FlashcardRepository()

    // --- Estados da UI ---
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _flashcardsState = MutableStateFlow<FlashcardsUiState>(FlashcardsUiState.Loading)
    val flashcardsState: StateFlow<FlashcardsUiState> = _flashcardsState.asStateFlow()

    private val _cardToEdit = MutableStateFlow<FlashcardDTO?>(null)
    val cardToEdit: StateFlow<FlashcardDTO?> = _cardToEdit.asStateFlow()

    private var currentSession: DeckSessionManager? = null

    private var currentSessionDeckId: String? = null

    // --- Funções de Carregamento ---
    fun loadFlashcards(deckId: String) {
        viewModelScope.launch {
            _flashcardsState.value = FlashcardsUiState.Loading
            repository.getFlashcards(deckId).collect { result ->
                result.onSuccess { flashcards ->
                    loadDeckStatistics(deckId)
                    _flashcardsState.value = FlashcardsUiState.Success(flashcards)
                }.onFailure { error ->
                    _flashcardsState.value =
                        FlashcardsUiState.Error(error.message ?: "Erro ao carregar flashcards.")
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

    fun saveFrenteVerso(
        deckId: String,
        frente: String,
        verso: String,
        imagemUri: Uri?,
        audioUri: Uri?
    ) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            val imageUrl =
                if (imagemUri != null) repository.uploadFile(imagemUri).getOrNull() else null
            val audioUrl =
                if (audioUri != null) repository.uploadFile(audioUri).getOrNull() else null
            val dto = FlashcardDTO(
                type = FlashcardTypeEnum.FRENTE_VERSO.name,
                frente = frente,
                verso = verso,
                perguntaImageUrl = imageUrl,
                perguntaAudioUrl = audioUrl
            )
            save(deckId, dto)
        }
    }

    fun saveCloze(deckId: String, texto: String, respostasMap: Map<String, String>) {
        val dto = FlashcardDTO(
            type = FlashcardTypeEnum.CLOZE.name,
            textoComLacunas = texto,
            respostasCloze = respostasMap
        )
        save(deckId, dto)
    }

    fun saveDigiteResposta(
        deckId: String,
        pergunta: String,
        respostasList: List<String>,
        imagemUri: Uri?,
        audioUri: Uri?
    ) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            val imageUrl =
                if (imagemUri != null) repository.uploadFile(imagemUri).getOrNull() else null
            val audioUrl =
                if (audioUri != null) repository.uploadFile(audioUri).getOrNull() else null
            val dto = FlashcardDTO(
                type = FlashcardTypeEnum.DIGITE_RESPOSTA.name,
                pergunta = pergunta,
                respostasValidas = respostasList,
                perguntaImageUrl = imageUrl,
                perguntaAudioUrl = audioUrl
            )
            save(deckId, dto)
        }
    }

    fun saveMultiplaEscolha(
        deckId: String,
        pergunta: String,
        perguntaImagemUri: Uri?,
        perguntaAudioUri: Uri?,
        alternativasUris: List<Pair<String, Uri?>>,
        respostaCorretaIndex: Int
    ) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            try {
                val perguntaImageDeferred =
                    async { if (perguntaImagemUri != null) repository.uploadFile(perguntaImagemUri) else null }
                val perguntaAudioDeferred =
                    async { if (perguntaAudioUri != null) repository.uploadFile(perguntaAudioUri) else null }
                val alternativasDeferred = alternativasUris.map { (_, uri) ->
                    async {
                        if (uri != null) repository.uploadFile(uri) else null
                    }
                }

                val perguntaImageUrl = perguntaImageDeferred.await()?.getOrNull()
                val perguntaAudioUrl = perguntaAudioDeferred.await()?.getOrNull()
                val alternativasUrls = alternativasDeferred.awaitAll().map { it?.getOrNull() }

                val alternativas = alternativasUris.mapIndexed { index, (texto, _) ->
                    AlternativaDTO(
                        text = texto.ifBlank { null },
                        imageUrl = alternativasUrls[index]
                    )
                }
                val dto = FlashcardDTO(
                    type = FlashcardTypeEnum.MULTIPLA_ESCOLHA.name,
                    pergunta = pergunta,
                    perguntaImageUrl = perguntaImageUrl,
                    perguntaAudioUrl = perguntaAudioUrl,
                    alternativas = alternativas,
                    respostaCorreta = alternativas[respostaCorretaIndex]
                )
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

    fun updateFrenteVerso(
        deckId: String,
        originalCard: FlashcardDTO,
        frente: String,
        verso: String
    ) {
        val updatedCard = originalCard.copy(frente = frente, verso = verso)
        updateFlashcard(deckId, updatedCard)
    }

    fun updateCloze(
        deckId: String,
        originalCard: FlashcardDTO,
        texto: String,
        respostasMap: Map<String, String>
    ) {
        val updatedCard = originalCard.copy(textoComLacunas = texto, respostasCloze = respostasMap)
        updateFlashcard(deckId, updatedCard)
    }

    fun updateDigiteResposta(
        deckId: String,
        originalCard: FlashcardDTO,
        pergunta: String,
        respostasList: List<String>
    ) {
        val updatedCard = originalCard.copy(pergunta = pergunta, respostasValidas = respostasList)
        updateFlashcard(deckId, updatedCard)
    }

    fun updateMultiplaEscolha(
        deckId: String,
        originalCard: FlashcardDTO,
        pergunta: String,
        alternativas: List<AlternativaDTO>,
        respostaCorretaIndex: Int
    ) {
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

    fun startDeckSession(deckId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        currentSessionDeckId = deckId
        currentSession = DeckSessionManager(deckId, uid)
    }

    fun addResultFrenteVerso(cardId: String) {
        currentSession?.addFrenteVerso(cardId)
    }

    fun addResultMultiplaEscolha(cardId: String, isCorrect: Boolean) {
        currentSession?.addMultiplaEscolha(cardId, isCorrect)
    }

    fun addResultCloze(
        cardId: String,
        blanksCorrect: Int? = null,
        blanksTotal: Int? = null,
        aiScore: Double? = null
    ) {
        currentSession?.addCloze(cardId, blanksCorrect, blanksTotal, aiScore)
    }

    fun addResultDigite(cardId: String, aiScore: Double) {
        currentSession?.addDigiteResposta(cardId, aiScore)
    }

    fun finishAndSaveDeckSession() {
        val session = currentSession?.build() ?: return
        viewModelScope.launch {
            repository.saveDeckSessionStat(session)
        }
        currentSession = null
        currentSessionDeckId = null
    }


    private fun fetchMyDecksFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Log.e("MyDecksApi", "Usuário não está logado.")
                return@launch
            }

            user.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.token
                    if (token == null) {
                        Log.e("MyDecksApi", "Não foi possível obter o token.")
                        return@addOnCompleteListener
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        val url =
                            URL("https://estudapp-api-293741035243.southamerica-east1.run.app/my-decks")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Authorization", "Bearer $token")


                        try {
                            val reader = BufferedReader(InputStreamReader(connection.inputStream))
                            val response = StringBuilder()
                            reader.forEachLine { response.append(it) }
                            Log.d("MyDecksApi", "Sucesso: $response")
                        } catch (e: Exception) {
                            Log.e("MyDecksApi", "Erro na requisição: ${e.message}")
                            val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                            Log.e("MyDecksApi", "Resposta do erro: $errorResponse")
                        } finally {
                            connection.disconnect()
                        }
                    }
                } else {
                    Log.e("MyDecksApi", "Falha ao obter token: ${task.exception?.message}")
                }
            }
        }
    }

    fun loadDeckStatistics(deckId: String) {
        viewModelScope.launch {
            Log.d("DeckStats", "Buscando estatísticas para o deck: $deckId")

            repository.getDeckSessions(deckId).collect { result ->
                result.onSuccess { sessions ->
                    if (sessions.isEmpty()) {
                        Log.d("DeckStats", "Nenhuma sessão de estudo encontrada para este deck.")
                        return@onSuccess
                    }

                    Log.d("DeckStats", "--- INÍCIO DAS ESTATÍSTICAS DO DECK ---")
                    sessions.forEachIndexed { index, session ->
                        Log.d("DeckStats", "  Sessão #${index + 1}:")
                        Log.d("DeckStats", "    - ID da Sessão: ${session.id}")
                        Log.d("DeckStats", "    - Data: ${java.util.Date(session.startedAt ?: 0)}")
                        Log.d(
                            "DeckStats",
                            "    - Pontuação: ${session.totalScore} / ${session.totalPossible}"
                        )
                        Log.d(
                            "DeckStats",
                            "    - Questões Respondidas: ${session.gradedQuestions} de ${session.totalQuestions}"
                        )
                        session.latitude?.let { lat ->
                            Log.d(
                                "DeckStats",
                                "    - Localização: Lat ${lat}, Lng ${session.longitude}"
                            )
                        }
                    }
                    Log.d("DeckStats", "--- FIM DAS ESTATÍSTICAS DO DECK ---")

                }.onFailure { error ->
                    Log.e("DeckStats", "Erro ao buscar estatísticas: ${error.message}")
                }
            }
        }
    }
    suspend fun validateDigiteResposta(
        deckId: String,
        flashcardId: String,
        userAnswer: String
    ): Result<ValidationResponse> {
        return withContext(Dispatchers.IO) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                return@withContext Result.failure(Exception("Usuário não está logado."))
            }

            val token = try {
                Tasks.await(user.getIdToken(true)).token
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Não foi possível obter o token."))
            }

            if (token == null) {
                return@withContext Result.failure(Exception("Token é nulo."))
            }

            val url = URL("https://estudapp-api-293741035243.southamerica-east1.run.app/flashcards/validate")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true

            try {
                val jsonPayload = JSONObject()
                jsonPayload.put("deckId", deckId)
                jsonPayload.put("flashcardId", flashcardId)
                jsonPayload.put("userAnswer", userAnswer)

                val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write(jsonPayload.toString())
                outputStreamWriter.flush()

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                reader.forEachLine { response.append(it) }

                val jsonResponse = JSONObject(response.toString())
                val isCorrect = jsonResponse.getBoolean("isCorrect")
                val score = jsonResponse.getDouble("score")

                Result.success(ValidationResponse(isCorrect, score))

            } catch (e: Exception) {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                Log.e("ValidateApi", "Erro na requisição: ${e.message}, Resposta: $errorResponse")
                Result.failure(e)
            } finally {
                connection.disconnect()
            }
        }
    }
    
    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
        }
    }
}

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