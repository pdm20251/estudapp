package com.example.estudapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class FlashcardDTO(
    var id: String = "",
    var deckId: String = "", // <-- ESTE CAMPO PRECISA ESTAR AQUI
    var type: String = FlashcardTypeEnum.FRENTE_VERSO.name,
    var userId: String = "",

    // Campos para Frente e Verso
    val frente: String? = null,
    val verso: String? = null,

    // Campos para Cloze (Omissão de Palavras)
    val textoComLacunas: String? = null,
    val respostasCloze: Map<String, String>? = null,

    // Campos para Digite a Resposta
    val pergunta: String? = null,
    val respostasValidas: List<String>? = null,

    // Campos para Múltipla Escolha
    val alternativas: List<String>? = null,
    val respostaCorreta: String? = null,

    // Campos para o Algoritmo de Repetição Espaçada
    var proximaRevisaoTimestamp: Long = System.currentTimeMillis(),
    var fatorFacilidade: Float = 2.5f,
    var repeticoes: Int = 0,
    var intervaloEmDias: Int = 1
) {
    // Construtor vazio exigido pelo Firebase
    constructor() : this(
        id = "",
        deckId = "", // <-- E AQUI TAMBÉM
        type = FlashcardTypeEnum.FRENTE_VERSO.name,
        userId = "",
        frente = null,
        verso = null,
        textoComLacunas = null,
        respostasCloze = null,
        pergunta = null,
        respostasValidas = null,
        alternativas = null,
        respostaCorreta = null,
        proximaRevisaoTimestamp = System.currentTimeMillis(),
        fatorFacilidade = 2.5f,
        repeticoes = 0,
        intervaloEmDias = 1
    )
}