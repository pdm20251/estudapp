package com.example.estudapp.data.model

data class ChatSessionDTO(
    var sessionId: String? = null,
    var userId: String? = null,
    var deckId: String? = null,      // opcional: vincule à jogada/deck
    var createdAt: Long? = null,
    var updatedAt: Long? = null,
    var status: String? = "open"     // "open" | "closed"
)