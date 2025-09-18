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
import java.util.UUID
import com.example.estudapp.data.model.DeckPlayStatDTO
import com.example.estudapp.data.model.ReviewResultDTO
import com.example.estudapp.data.model.SimpleChatMessageDTO
import com.google.firebase.database.ServerValue
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


class FlashcardRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance() // Referência para o Storage
    private val decksRef = database.getReference("decks")
    private val flashcardsRef = database.getReference("flashcards")

    private val statsRef = database.getReference("stats")

    private val chatsRef = database.getReference("chats")

    private val usersRef = database.getReference("users")

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
// Modifique sua função deleteFlashcard para ficar assim:
    suspend fun deleteFlashcard(deckId: String, flashcardId: String): Result<Unit> {
        return try {
            val flashcardRef = flashcardsRef.child(deckId).child(flashcardId)
            flashcardRef.removeValue().await()

            // --- ADICIONE A CHAMADA AQUI ---
            updateDeckCardCount(deckId, -1) // Decrementa em 1

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

    suspend fun deleteDeck(deckId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))

            // 1) Leia os flashcards do deck (para montar o multi-update)
            val cardsSnap = flashcardsRef.child(deckId).get().await()

            // 2) Monte um fan-out update: remove o deck do usuário e cada card do deck
            val updates = hashMapOf<String, Any?>()
            updates["decks/$userId/$deckId"] = null
            for (card in cardsSnap.children) {
                val cardId = card.key ?: continue
                updates["flashcards/$deckId/$cardId"] = null
            }

            // (Opcional) tentar limpar o nó vazio do deck em flashcards/
            // Se sua regra permitir, ótimo; se não permitir, ignoramos a falha.
            // updates["flashcards/$deckId"] = null

            // 3) Execute a remoção atômica
            FirebaseDatabase.getInstance().reference.updateChildren(updates).await()

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

            // --- ADICIONE A CHAMADA AQUI ---
            updateDeckCardCount(deckId, 1) // Incrementa em 1

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

    suspend fun saveDeckSessionStat(session: DeckPlayStatDTO): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))
            val deckId = session.deckId ?: return Result.failure(IllegalArgumentException("deckId ausente."))
            // gera um push id p/ sessão
            val newSessionRef = statsRef.child(userId).child(deckId).push()
            val sessionId = newSessionRef.key!!

            // garante campos obrigatórios
            session.id = sessionId
            session.userId = userId

            // persiste a sessão completa
            newSessionRef.setValue(session).await()
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDeckSessions(deckId: String): kotlinx.coroutines.flow.Flow<Result<List<DeckPlayStatDTO>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("Usuário não autenticado.")))
            awaitClose(); return@callbackFlow
        }
        val ref = statsRef.child(userId).child(deckId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(DeckPlayStatDTO::class.java) }
                trySend(Result.success(items))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener)}
    }


    suspend fun getFlashcardsOnce(deckId: String): Result<List<FlashcardDTO>> {
        return try {
            val snapshot = flashcardsRef.child(deckId).get().await()
            val items = snapshot.children.mapNotNull { it.getValue(FlashcardDTO::class.java) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun updateDeckCardCount(deckId: String, increment: Int) {
        val userId = getCurrentUserId() ?: return
        val deckRef = decksRef.child(userId).child(deckId).child("cardCount")

        // A função foi reescrita para aguardar a transação ser concluída
        suspendCancellableCoroutine<Unit> { continuation ->
            deckRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentCount = currentData.getValue(Int::class.java) ?: 0
                    val newCount = currentCount + increment
                    currentData.value = if (newCount < 0) 0 else newCount
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        // Se a transação falhou, informa a coroutine sobre o erro
                        continuation.resumeWithException(error.toException())
                    } else {
                        // Se a transação foi bem-sucedida, informa a coroutine para continuar
                        continuation.resume(Unit)
                    }
                }
            })
        }
    }

    // --- Funções para o CHAT SIMPLIFICADO (Versão Direta, sem Sessão) ---

    /**
     * Envia uma mensagem diretamente para o nó do utilizador, sem uma camada de sessão.
     * O ID da mensagem é a chave principal.
     */
    suspend fun sendDirectMessage(text: String): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))

            val userChatRef = chatsRef.child(userId)

            val messageRef = userChatRef.push()
            val messageId = messageRef.key!!

            val message = SimpleChatMessageDTO(
                id = messageId,
                sender = "USER",
                text = text
            )

            messageRef.setValue(message).await()
            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa todas as mensagens de um utilizador.
     */
    fun observeUserMessages(): Flow<Result<List<SimpleChatMessageDTO>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("Usuário não autenticado.")))
            awaitClose(); return@callbackFlow
        }

        val userChatRef = chatsRef.child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(SimpleChatMessageDTO::class.java) }
                trySend(Result.success(messages))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        userChatRef.addValueEventListener(listener)
        awaitClose { userChatRef.removeEventListener(listener) }
    }

    //Salvar nome em ''users'' no realtime database
    /* suspend fun saveUserName(userId: String, userName: String): Result<Unit> {
        return try {
            usersRef.child(userId).child("user_name").setValue(userName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
*/
}