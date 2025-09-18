package com.example.estudapp.ui.feature.flashcard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.AlternativaDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.data.model.SimpleChatMessageDTO
import com.example.estudapp.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel : ViewModel() {

    private val repository = FlashcardRepository()
    private val flashcardViewModel = FlashcardViewModel()

    // --- Variáveis de Estado para a Sessão de Estudo ---
    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    private var allCardsInDeck: List<FlashcardDTO> = emptyList()
    private var currentCardIndex = 0

    // --- Variáveis de Estado e Lógica para o CHAT (Versão Direta, sem Sessão) ---
    private val _messages = MutableStateFlow<List<SimpleChatMessageDTO>>(emptyList())
    val messages: StateFlow<List<SimpleChatMessageDTO>> = _messages.asStateFlow()

    var currentSessionTotalScore: Double? = 0.0
    var currentSessionPossibleScore: Double? = 0.0

    private val _scoreValues = MutableStateFlow<List<Double>>(listOf(0.0, 0.0))
    val scoreValues: StateFlow<List<Double>> = _scoreValues.asStateFlow()


    init {
        // Começa a ouvir por mensagens do utilizador assim que o ViewModel é criado
        observeMessages()
    }

    /**
     * Ouve todas as mensagens do utilizador logado.
     */
    private fun observeMessages() {
        viewModelScope.launch {
            repository.observeUserMessages().collect { result ->
                result.onSuccess { messageList ->
                    _messages.value = messageList
                }.onFailure {
                    Log.e("Chat", "Erro ao observar mensagens: ${it.message}")
                }
            }
        }
    }

    /**
     * Envia uma nova mensagem.
     */
    fun sendMessage(text: String) {
        viewModelScope.launch {
            repository.sendDirectMessage(text).onFailure {
                Log.e("Chat", "Erro ao enviar mensagem: ${it.message}")
            }
        }
    }


    // --- Funções da Sessão de Estudo ---
    fun startStudySession(deckId: String) {
        viewModelScope.launch {
            _uiState.value = StudyUiState.Loading
            // Usamos a nova função que busca os dados apenas uma vez
            val result = repository.getFlashcardsOnce(deckId)

            result.onSuccess { flashcards ->
                allCardsInDeck = flashcards.shuffled()
                currentCardIndex = 0
                if (allCardsInDeck.isNotEmpty()) {
                    flashcardViewModel.startDeckSession(deckId)
                    _uiState.value = StudyUiState.Studying(card = allCardsInDeck[currentCardIndex], isShowingAnswer = false)
                } else {
                    _uiState.value = StudyUiState.EmptyDeck
                }
            }.onFailure {
                _uiState.value = StudyUiState.Error(it.message ?: "Erro ao carregar cards.")
            }
        }
    }

    fun checkAnswer(
        userAnswer: String,
        clozeAnswers: Map<String, String> = emptyMap(),
        multipleChoiceAnswer: AlternativaDTO? = null
    ) {
        val currentState = _uiState.value
      
        // Trava a função se já houver uma verificação em andamento
        if (currentState is StudyUiState.Studying && !_isProcessing.value) {
            _isProcessing.value = true // Inicia o travamento

            viewModelScope.launch {
                try {
                    val card = currentState.card
                    var isOverallCorrect = false
                    val clozeFeedbackMap = mutableMapOf<String, Boolean>()

                    when (card.type) {
                        FlashcardTypeEnum.DIGITE_RESPOSTA.name -> {
                            val validationResult = flashcardViewModel.validateDigiteResposta(
                                deckId = card.deckId,
                                flashcardId = card.id,
                                userAnswer = userAnswer
                            )

                            validationResult.onSuccess { response ->
                                isOverallCorrect = response.isCorrect
                                flashcardViewModel.addResultDigite(card.id, response.score)
                            }.onFailure {
                                isOverallCorrect = false
                                flashcardViewModel.addResultDigite(card.id, 0.0)
                                Log.e("StudyViewModel", "Falha ao validar resposta: ${it.message}")
                            }
                        }
                        FlashcardTypeEnum.MULTIPLA_ESCOLHA.name -> {
                            isOverallCorrect = card.respostaCorreta == multipleChoiceAnswer
                            flashcardViewModel.addResultMultiplaEscolha(card.id, isOverallCorrect)
                        }
                        FlashcardTypeEnum.CLOZE.name -> {
                            card.respostasCloze?.forEach { (key, correctAnswer) ->
                                val userClozeAnswer = clozeAnswers[key] ?: ""
                                clozeFeedbackMap[key] = correctAnswer.equals(userClozeAnswer, ignoreCase = true)
                            }
                            isOverallCorrect = clozeFeedbackMap.values.all { it }
                            val correctCount = clozeFeedbackMap.values.count { it }
                            val totalCount = clozeFeedbackMap.size
                            flashcardViewModel.addResultCloze(card.id, correctCount, totalCount)
                        }
                        // Adicionado para lidar com o tipo FRENTE_VERSO, embora não seja "verificável"
                        FlashcardTypeEnum.FRENTE_VERSO.name -> {
                            // Não há nada para verificar, a ação é 'showAnswer'
                        }
                    }

                    _uiState.value = currentState.copy(
                        isShowingAnswer = true,
                        wasCorrect = isOverallCorrect,
                        clozeFeedback = if (card.type == FlashcardTypeEnum.CLOZE.name) clozeFeedbackMap else null
                    )
                } finally {
                    _isProcessing.value = false // Libera o travamento no final
                }
            }
        }
    }

    fun showAnswer() {
        // Previne a execução se outra ação estiver em andamento
        if (_isProcessing.value) return

        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            _uiState.value = currentState.copy(isShowingAnswer = true, wasCorrect = null)
        }
    }

    fun nextCard() {
        // Previne a execução se outra ação estiver em andamento
        if (_isProcessing.value) return
        _isProcessing.value = true // Inicia o travamento

        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            processFlashcardReview(currentState.card, currentState.wasCorrect)

            if (currentState.card.type == FlashcardTypeEnum.FRENTE_VERSO.name) {
                flashcardViewModel.addResultFrenteVerso(currentState.card.id)
            }
        }

        currentCardIndex++

        if (currentCardIndex < allCardsInDeck.size) {
            _uiState.value = StudyUiState.Studying(
                card = allCardsInDeck[currentCardIndex],
                isShowingAnswer = false,
                wasCorrect = null,
                clozeFeedback = null
            )
        } else {
            var list = flashcardViewModel.getScoreValues()
            _scoreValues.value = ArrayList(list)

            flashcardViewModel.finishAndSaveDeckSession()
            _uiState.value = StudyUiState.SessionFinished
        }

        _isProcessing.value = false // Libera o travamento
    }
    private fun processFlashcardReview(card: FlashcardDTO, wasCorrect: Boolean?) {
        viewModelScope.launch {
            // Simplesmente incrementa o número de repetições
            val updatedRepeticoes = card.repeticoes + 1

            // (Lógica futura de repetição espaçada poderia vir aqui,
            //  calculando o novo intervalo e a próxima data de revisão)

            val updatedCard = card.copy(
                repeticoes = updatedRepeticoes
            )

            // Usa a função de atualização do repositório para salvar o card modificado
            repository.updateFlashcard(updatedCard.deckId, updatedCard)
        }
    }
}

sealed class StudyUiState {
    object Loading : StudyUiState()
    data class Studying(
        val card: FlashcardDTO,
        val isShowingAnswer: Boolean,
        val wasCorrect: Boolean? = null,
        val clozeFeedback: Map<String, Boolean>? = null
    ) : StudyUiState()
    object EmptyDeck : StudyUiState()
    object SessionFinished : StudyUiState()
    data class Error(val message: String) : StudyUiState()
}