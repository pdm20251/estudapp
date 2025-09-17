package com.example.estudapp.navigate

object Routes {
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"

    // Tela principal pós-login
    const val DECK_LIST = "deck_list"

    const val CREATE_DECK = "create_deck"

    // Flashcards
    const val FLASHCARD_LIST = "flashcard_list/{deckId}"
    const val CREATE_FLASHCARD = "create_flashcard/{deckId}"
    const val EDIT_FLASHCARD = "edit_flashcard/{deckId}/{cardId}"
    const val STUDY_SESSION = "study_session/{deckId}"

    // Helpers para construir rotas dinâmicas
    fun flashcardList(deckId: String) = "flashcard_list/$deckId"
    fun createFlashcard(deckId: String) = "create_flashcard/$deckId"
    fun editFlashcard(deckId: String, cardId: String) = "edit_flashcard/$deckId/$cardId"
    fun studySession(deckId: String) = "study_session/$deckId"
}