package com.example.estudapp.data.model

data class DeckPlayStatDTO(
    var id: String? = null,           // sessionId (push key)
    var deckId: String? = null,
    var userId: String? = null,

    var startedAt: Long? = null,      // epoch millis
    var finishedAt: Long? = null,     // epoch millis

    var totalScore: Double? = 0.0,    // soma dos scores
    var totalPossible: Double? = 0.0, // soma dos maxScore
    var totalQuestions: Int? = 0,     // qtd total (inclui frente/verso)
    var gradedQuestions: Int? = 0,    // qtd que valem nota

    // NOVO
    var latitude: Double? = null,
    var longitude: Double? = null,

    // results[cardId] = ReviewResultDTO
    var results: Map<String, ReviewResultDTO>?=null
)