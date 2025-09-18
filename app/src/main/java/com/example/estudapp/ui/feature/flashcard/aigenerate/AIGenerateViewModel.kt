package com.example.estudapp.ui.feature.flashcard.aigenerate

import android.util.Log
import androidx.activity.result.launch
import androidx.annotation.NonNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.ui.feature.flashcard.StudyUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class AIGenerateViewModel : ViewModel() {
    private val client = OkHttpClient.Builder()
        .build()

    private val _uiState = MutableStateFlow<GenUiState>(GenUiState.Waiting)
    val uiState: StateFlow<GenUiState> = _uiState.asStateFlow()

    fun generateFlashcardFromAI(
        deckId: String,
        cardType: String, // e.g., "DIGITE_RESPOSTA"
        userComment: String // e.g., "Faça uma pergunda sobre os Cazuza"
    ) {
        viewModelScope.launch {
            _uiState.value = GenUiState.Loading
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Log.e("AIGenerateVM", "User not authenticated.")
                _uiState.value = GenUiState.Error("Error: User not authenticated")
                return@launch
            }

            try {
                val tokenResult: GetTokenResult = user.getIdToken(true).await()
                val idToken = tokenResult.token
                if (idToken == null) {
                    Log.e("AIGenerateVM", "Failed to get ID token.")
                    _uiState.value = GenUiState.Error("Error: Failed to get ID token")
                    return@launch
                }

                val jsonPayload = JSONObject()
                jsonPayload.put("type", cardType)
                jsonPayload.put("userComment", userComment)

                val requestBody = jsonPayload.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val url = "https://estudapp-api-293741035243.southamerica-east1.run.app/decks/$deckId/flashcards/generate"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $idToken")
                    .addHeader("Content-Type", "application/json")
                    .build()
                Log.d("AIGenerateVM", "Making request to: $url")
                Log.d("AIGenerateVM", "With token: Bearer $idToken")
                Log.d("AIGenerateVM", "With payload: ${jsonPayload.toString()}")

                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val responseBodyString = response.body?.string()
                        if (response.isSuccessful) {
                            Log.i("AIGenerateVM", "Flashcard generation successful: $responseBodyString")
                            _uiState.value = GenUiState.Completed
                            // TODO: Parse the responseBodyString if it contains useful data (e.g., the generated flashcard ID)
                        } else {
                            Log.e("AIGenerateVM", "Flashcard generation failed: ${response.code} - $responseBodyString")
                            _uiState.value = GenUiState.Error("Error: ${response.code} - ${responseBodyString ?: "Unknown error"}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AIGenerateVM", "Exception during flashcard generation: ${e.message}", e)
                _uiState.value = GenUiState.Error("Exception: ${e.message}")
            }
        }
    }
}

sealed class GenUiState {
    object Waiting : GenUiState()
    object Loading : GenUiState()
    object Completed : GenUiState()
    data class Error(val message: String) : GenUiState()
}