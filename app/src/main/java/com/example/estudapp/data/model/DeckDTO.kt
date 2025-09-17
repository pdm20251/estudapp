package com.example.estudapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DeckDTO(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var cardCount: Int = 0,
    var userId: String = "",
    // --- CAMPO ADICIONADO ---
    var proximaRevisaoTimestamp: Long? = null // Armazena a data como um timestamp
) {
    // Construtor vazio exigido pelo Firebase, agora com o novo campo
    constructor() : this("", "", null, 0, "", null)
}