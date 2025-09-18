package com.example.estudapp.data.model

import com.google.firebase.database.ServerValue

data class SimpleChatMessageDTO(
    var id: String? = null,
    var sender: String? = null,
    var text: String? = null,
    val timestamp: Any? = ServerValue.TIMESTAMP
) {
    constructor() : this(null, null, null, null)
}