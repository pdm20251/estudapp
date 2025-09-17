package com.example.estudapp.domain.repository

import android.net.Uri
import com.example.estudapp.data.model.DeckDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FlashcardRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance() // Referência para o Storage
    private val decksRef = database.getReference("decks")
    private val flashcardsRef = database.getReference("flashcards")

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getFlashcard(deckId: String, flashcardId: String): Result<FlashcardDTO?> {
        return try {
            val snapshot = flashcardsRef.child(deckId).child(flashcardId).get().await()
            val flashcard = snapshot.getValue(FlashcardDTO::class.java)
            Result.success(flashcard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //Função para editar flashcards
    suspend fun updateFlashcard(deckId: String, flashcard: FlashcardDTO): Result<Unit> {
        return try {
            // A ID do flashcard já existe, então pegamos a referência direta
            val flashcardRef = flashcardsRef.child(deckId).child(flashcard.id)
            // O setValue com uma referência existente sobrescreve os dados (atualiza)
            flashcardRef.setValue(flashcard).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //Lixeira para deletar flashcard
    suspend fun deleteFlashcard(deckId: String, flashcardId: String): Result<Unit> {
        return try {
            // Cria a referência direta para o flashcard que queremos apagar
            val flashcardRef = flashcardsRef.child(deckId).child(flashcardId)
            // Manda o Firebase remover o valor (deletar o nó)
            flashcardRef.removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- NOVA FUNÇÃO DE UPLOAD DE ARQUIVO ---
    suspend fun uploadFile(uri: Uri): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))
            // Cria um nome de arquivo único para evitar sobreposições
            val fileName = UUID.randomUUID().toString()
            val fileRef = storage.reference.child("$userId/$fileName")

            // Faz o upload do arquivo
            fileRef.putFile(uri).await()

            // Obtém a URL de download do arquivo após o upload
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // --- Funções de Deck (sem alterações) ---

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

    // --- Funções de Flashcard (sem alterações) ---

    suspend fun saveFlashcard(deckId: String, flashcard: FlashcardDTO): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))
            flashcard.userId = userId
            flashcard.deckId = deckId
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