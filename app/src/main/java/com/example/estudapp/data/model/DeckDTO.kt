package com.example.estudapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DeckDTO(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var cardCount: Int = 0,
    var userId: String = ""
) {
    // Construtor vazio exigido pelo Firebase
    constructor() : this("", "", null, 0, "")
}