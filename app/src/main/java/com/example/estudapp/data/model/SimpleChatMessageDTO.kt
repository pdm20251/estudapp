package com.example.estudapp.data.model

import com.google.firebase.database.ServerValue

// DTO para a estrutura de chat simplificada
data class SimpleChatMessageDTO(
    var id: String? = null,
    var sender: String? = null, // Ex: "USER" ou "ASSISTANT"
    var text: String? = null,
    val timestamp: Any? = ServerValue.TIMESTAMP // Usa o timestamp do servidor Firebase
) {
    // Construtor vazio necessário para o Firebase
    constructor() : this(null, null, null, null)
}