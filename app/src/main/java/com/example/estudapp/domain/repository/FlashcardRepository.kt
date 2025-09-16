package com.example.estudapp.domain.repository

import com.example.estudapp.data.model.DeckDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FlashcardRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val decksRef = database.getReference("decks")
    private val flashcardsRef = database.getReference("flashcards")

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // --- Funções de Deck ---

    suspend fun saveDeck(name: String, description: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))
            val newDeckRef = decksRef.child(userId).push()
            val deck = DeckDTO(
                id = newDeckRef.key!!,
                name = name,
                description = description,
                userId = userId
            )
            newDeckRef.setValue(deck).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDecks(): Flow<Result<List<DeckDTO>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("Usuário não autenticado.")))
            awaitClose()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(DeckDTO::class.java) }
                trySend(Result.success(items))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        decksRef.child(userId).addValueEventListener(listener)
        awaitClose { decksRef.child(userId).removeEventListener(listener) }
    }

    // --- Funções de Flashcard ---

    suspend fun saveFlashcard(deckId: String, flashcard: FlashcardDTO): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))
            flashcard.userId = userId
            flashcard.deckId = deckId // Esta linha dará erro se o passo 1 não for feito
            val newFlashcardRef = flashcardsRef.child(deckId).push()
            flashcard.id = newFlashcardRef.key!!
            newFlashcardRef.setValue(flashcard).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFlashcards(deckId: String): Flow<Result<List<FlashcardDTO>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(FlashcardDTO::class.java) }
                trySend(Result.success(items))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        flashcardsRef.child(deckId).addValueEventListener(listener)
        awaitClose { flashcardsRef.child(deckId).removeEventListener(listener) }
    }
}