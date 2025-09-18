package com.example.estudapp.data.model

data class ReviewResultDTO(
    var cardId: String? = null,
    var type: String? = null,         // FlashcardTypeEnum.name
    var score: Double? = null,        // 0..10 (pode ter casas decimais)
    var maxScore: Double? = null      // 0..10
)