package com.example.estudapp.domain.repository

import com.example.estudapp.data.model.FavoriteLocationDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationRepository {

    private val auth = FirebaseAuth.getInstance()
    // A referência principal será 'users', e salvaremos as localizações dentro de cada usuário
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Salva uma nova localização para o usuário logado
    suspend fun saveFavoriteLocation(name: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuário não autenticado."))

            // Criamos um nó para as localizações dentro do usuário
            val locationsRef = usersRef.child(userId).child("favoriteLocations").push()

            val location = FavoriteLocationDTO(
                id = locationsRef.key!!,
                name = name,
                latitude = latitude,
                longitude = longitude,
                userId = userId
            )

            locationsRef.setValue(location).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ouve em tempo real as localizações do usuário logado
    fun getFavoriteLocations(): Flow<Result<List<FavoriteLocationDTO>>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.failure(Exception("Usuário não autenticado.")))
            awaitClose()
            return@callbackFlow
        }

        val locationsRef = usersRef.child(userId).child("favoriteLocations")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(FavoriteLocationDTO::class.java) }
                trySend(Result.success(items))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }

        locationsRef.addValueEventListener(listener)
        awaitClose { locationsRef.removeEventListener(listener) }
    }
}