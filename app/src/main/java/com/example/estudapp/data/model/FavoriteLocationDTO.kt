package com.example.estudapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class FavoriteLocationDTO(
    var id: String = "",
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var radius: Int = 100,
    var userId: String = ""
) {
    // Construtor vazio exigido pelo Firebase
    constructor() : this("", "", 0.0, 0.0, 100, "")
}