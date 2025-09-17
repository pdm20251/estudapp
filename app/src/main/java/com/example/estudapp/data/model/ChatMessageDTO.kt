package com.example.estudapp.data.model

data class ChatMessageDTO(
    var id: String? = null,
    var role: String? = null,        // "user" | "assistant" | "system"
    var content: String? = null,
    var status: String? = "queued",  // "queued" | "answering" | "answered" | "error"
    var createdAt:Long?=null
)