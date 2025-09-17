package com.example.estudapp.ui.feature.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.AlternativaDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel : ViewModel() {

    private val repository = FlashcardRepository()

    private val flashcardViewModel = FlashcardViewModel()
    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var allCardsInDeck: List<FlashcardDTO> = emptyList()
    private var currentCardIndex = 0

    fun startStudySession(deckId: String) {
        viewModelScope.launch {
            _uiState.value = StudyUiState.Loading
            // --- MUDANÇA PRINCIPAL AQUI ---
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
        if (currentState is StudyUiState.Studying) {
            val card = currentState.card
            var isOverallCorrect = false
            val clozeFeedbackMap = mutableMapOf<String, Boolean>()

            when (card.type) {
                // O CASO FRENTE_VERSO FOI REMOVIDO DAQUI
                FlashcardTypeEnum.DIGITE_RESPOSTA.name -> {
                    isOverallCorrect = card.respostasValidas?.any { it.equals(userAnswer, ignoreCase = true) } == true
                    flashcardViewModel.addResultDigite(card.id, if (isOverallCorrect) 10.0 else 0.0)
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
                    // --- ADICIONE A CHAMADA AQUI ---
                    val correctCount = clozeFeedbackMap.values.count { it }
                    val totalCount = clozeFeedbackMap.size
                    flashcardViewModel.addResultCloze(card.id, correctCount, totalCount)
                }
            }

            _uiState.value = currentState.copy(
                isShowingAnswer = true,
                wasCorrect = isOverallCorrect,
                clozeFeedback = if (card.type == FlashcardTypeEnum.CLOZE.name) clozeFeedbackMap else null
            )
        }
    }

    // NOVA FUNÇÃO ESPECÍFICA PARA MOSTRAR A RESPOSTA
    fun showAnswer() {
        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            // Apenas revela a resposta, sem verificar se está correta (wasCorrect = null)
            _uiState.value = currentState.copy(isShowingAnswer = true, wasCorrect = null)
        }
    }

    fun nextCard() {
        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            // 1. Processa a revisão para o card que acabamos de ver (incrementa "repeticoes")
            processFlashcardReview(currentState.card, currentState.wasCorrect)

            // 2. Registra o resultado da sessão para o "Frente e Verso"
            if (currentState.card.type == FlashcardTypeEnum.FRENTE_VERSO.name) {
                flashcardViewModel.addResultFrenteVerso(currentState.card.id)
            }
        }

        // 3. Avança o contador
        currentCardIndex++

        // 4. Verifica se a sessão terminou
        if (currentCardIndex < allCardsInDeck.size) {
            // Se AINDA HÁ cards, mostra o próximo
            _uiState.value = StudyUiState.Studying(card = allCardsInDeck[currentCardIndex], isShowingAnswer = false, wasCorrect = null, clozeFeedback = null)
        } else {
            // Se NÃO HÁ mais cards, finaliza a sessão
            flashcardViewModel.finishAndSaveDeckSession()
            _uiState.value = StudyUiState.SessionFinished
        }
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