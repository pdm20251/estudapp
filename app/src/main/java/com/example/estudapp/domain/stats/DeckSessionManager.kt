package com.example.estudapp.domain.stats

import com.example.estudapp.data.model.DeckPlayStatDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.data.model.ReviewResultDTO
import kotlin.math.round

/**
 * Agrega resultados de uma “jogada” (sessão) de um deck.
 */
class DeckSessionManager(
    private val deckId: String,
    private val userId: String
) {
    private val startedAt = System.currentTimeMillis()

    private val results = linkedMapOf<String, ReviewResultDTO>()
    private var totalScore = 0.0
    private var totalPossible = 0.0
    private var totalQuestions = 0
    private var gradedQuestions = 0

    private var latitude: Double? = null
    private var longitude: Double? = null

    /** Permite setar localização (ex.: GPS real ou valor aleatório para teste). */
    fun setLocation(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
    }

    /** Frente/Verso NÃO vale nota (0/0). */
    fun addFrenteVerso(cardId: String) {
        results[cardId] = ReviewResultDTO(
            cardId = cardId,
            type = FlashcardTypeEnum.FRENTE_VERSO.name,
            score = 0.0,
            maxScore = 0.0
        )
        totalQuestions += 1
    }

    /** Múltipla escolha → 10 se correto, 0 se errado. */
    fun addMultiplaEscolha(cardId: String, isCorrect: Boolean) {
        val score = if (isCorrect) 10.0 else 0.0
        results[cardId] = ReviewResultDTO(
            cardId = cardId,
            type = FlashcardTypeEnum.MULTIPLA_ESCOLHA.name,
            score = score,
            maxScore = 10.0
        )
        totalQuestions += 1
        gradedQuestions += 1
        totalScore += score
        totalPossible += 10.0
    }

    /** Cloze → pode ser por IA (aiScore) ou fração (acertos/total). */
    fun addCloze(cardId: String, blanksCorrect: Int? = null, blanksTotal: Int? = null, aiScore: Double? = null) {
        val score = when {
            aiScore != null -> aiScore.coerceIn(0.0, 10.0)
            blanksCorrect != null && blanksTotal != null && blanksTotal > 0 ->
                (blanksCorrect.toDouble() / blanksTotal.toDouble()) * 10.0
            else -> 0.0
        }
        results[cardId] = ReviewResultDTO(
            cardId = cardId,
            type = FlashcardTypeEnum.CLOZE.name,
            score = score,
            maxScore = 10.0
        )
        totalQuestions += 1
        gradedQuestions += 1
        totalScore += score
        totalPossible += 10.0
    }

    /** Digite resposta → nota 0..10 (geralmente da IA). */
    fun addDigiteResposta(cardId: String, aiScore: Double) {
        val score = aiScore.coerceIn(0.0, 10.0)
        results[cardId] = ReviewResultDTO(
            cardId = cardId,
            type = FlashcardTypeEnum.DIGITE_RESPOSTA.name,
            score = score,
            maxScore = 10.0
        )
        totalQuestions += 1
        gradedQuestions += 1
        totalScore += score
        totalPossible += 10.0
    }

    fun getTotalScore(): Double{
        return totalScore
    }

    fun getPossibleScore(): Double{
        return totalPossible
    }

    /** Monta DTO final para salvar no RTDB. */
    fun build(): DeckPlayStatDTO {
        val finishedAt = System.currentTimeMillis()
        return DeckPlayStatDTO(
            id = null,
            deckId = deckId,
            userId = userId,
            startedAt = startedAt,
            finishedAt = finishedAt,
            totalScore = round2(totalScore),
            totalPossible = round2(totalPossible),
            totalQuestions = totalQuestions,
            gradedQuestions = gradedQuestions,
            latitude = latitude,
            longitude = longitude,
            results = results.toMap()
        )
    }

    private fun round2(v: Double) = round(v * 100) / 100.0
}