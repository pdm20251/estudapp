package com.example.estudapp.ui.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudapp.data.model.SimpleChatMessageDTO
import com.example.estudapp.domain.repository.FlashcardRepository
import com.example.estudapp.ui.feature.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class ChatViewModel : ViewModel() {

    private val repository = FlashcardRepository()
    private val auth = FirebaseAuth.getInstance()

    // Estados da UI
    private val _messages = MutableStateFlow<List<SimpleChatMessageDTO>>(emptyList())
    val messages: StateFlow<List<SimpleChatMessageDTO>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Controla se estamos aguardando resposta da IA
    private var awaitingAIResponse = false
    private var lastUserMessageCount = 0

    init {
        // Carrega mensagens existentes ao inicializar
        observeMessages()
    }

    /**
     * Observa todas as mensagens do usuário em tempo real
     */
    private fun observeMessages() {
        viewModelScope.launch {
            repository.observeUserMessages().collect { result ->
                result.onSuccess { messageList ->
                    // Ordena mensagens por timestamp
                    val sortedMessages = messageList.sortedBy {
                        (it.timestamp as? Long) ?: 0L
                    }

                    // Verifica se recebemos uma nova mensagem da IA
                    if (awaitingAIResponse && sortedMessages.size > lastUserMessageCount) {
                        val lastMessage = sortedMessages.lastOrNull()
                        if (lastMessage?.sender == "ASSISTANT") {
                            // Recebemos resposta da IA, remove o loading
                            _isLoading.value = false
                            awaitingAIResponse = false
                            Log.d("ChatViewModel", "Resposta da IA recebida, removendo loading")
                        }
                    }

                    _messages.value = sortedMessages
                }.onFailure { error ->
                    Log.e("ChatViewModel", "Erro ao observar mensagens: ${error.message}")
                    _errorMessage.value = "Erro ao carregar mensagens: ${error.message}"
                    // Remove loading em caso de erro
                    _isLoading.value = false
                    awaitingAIResponse = false
                }
            }
        }
    }

    /**
     * Envia uma nova mensagem do usuário
     */
    fun sendMessage(text: String) {
        if (_isLoading.value) return // Previne múltiplos envios

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Marca que estamos aguardando resposta da IA
                awaitingAIResponse = true
                lastUserMessageCount = _messages.value.size + 1 // +1 pela mensagem que vamos enviar

                // 1. Salva a mensagem do usuário no Firebase
                val result = repository.sendDirectMessage(text)
                result.onSuccess { messageId ->
                    Log.d("ChatViewModel", "Mensagem do usuário salva com ID: $messageId")

                    // 2. Chama a API para processar a mensagem
                    // IMPORTANTE: Não remove o loading aqui, só quando a IA responder
                    callChatAPI()

                }.onFailure { error ->
                    Log.e("ChatViewModel", "Erro ao salvar mensagem: ${error.message}")
                    _errorMessage.value = "Erro ao enviar mensagem: ${error.message}"
                    _isLoading.value = false
                    awaitingAIResponse = false
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Erro inesperado ao enviar mensagem: ${e.message}")
                _errorMessage.value = "Erro inesperado: ${e.message}"
                _isLoading.value = false
                awaitingAIResponse = false
            }
        }
    }

    /**
     * Chama a API de chat para processar a mensagem
     */
    private suspend fun callChatAPI() {
        withContext(Dispatchers.IO) {
            val user = auth.currentUser
            if (user == null) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Usuário não está logado."
                    _isLoading.value = false
                    awaitingAIResponse = false
                }
                return@withContext
            }

            try {
                // Obtém o token de autenticação
                val tokenTask = user.getIdToken(true)
                val token = com.google.android.gms.tasks.Tasks.await(tokenTask).token

                if (token == null) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Não foi possível obter o token de autenticação."
                        _isLoading.value = false
                        awaitingAIResponse = false
                    }
                    return@withContext
                }

                // Configura a conexão HTTP
                val url = URL("https://estudapp-api-293741035243.southamerica-east1.run.app/chat/respond")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.doOutput = true
                connection.connectTimeout = 30000 // 30 segundos
                connection.readTimeout = 60000 // 60 segundos

                // Envia a requisição (sem body, pois a API processará as mensagens do Firebase)
                val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write("{}")
                outputStreamWriter.flush()
                outputStreamWriter.close()

                // Verifica a resposta
                val responseCode = connection.responseCode
                Log.d("ChatViewModel", "Response code da API: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // API processou com sucesso
                    // IMPORTANTE: NÃO remove o loading aqui!
                    // O loading só será removido quando o observer detectar a resposta da IA
                    Log.d("ChatViewModel", "API processou a mensagem com sucesso - aguardando resposta da IA no Firebase")
                } else {
                    // Lê a resposta de erro
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                    Log.e("ChatViewModel", "Erro na API: $responseCode - $errorResponse")

                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Erro do servidor: $responseCode"
                        _isLoading.value = false
                        awaitingAIResponse = false
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Erro na requisição para API: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Erro de conexão: ${e.message}"
                    _isLoading.value = false
                    awaitingAIResponse = false
                }
            }
        }
    }

    /**
     * Limpa mensagens de erro
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Força atualização das mensagens (se necessário)
     */
    fun refreshMessages() {
        // As mensagens são atualizadas automaticamente pelo observer,
        // mas esta função pode ser usada para debugging ou refresh manual
        Log.d("ChatViewModel", "Refresh solicitado - mensagens atuais: ${_messages.value.size}")
    }

    /**
     * Função para lidar com timeout de resposta da IA (opcional)
     */
    fun handleResponseTimeout() {
        if (awaitingAIResponse) {
            _isLoading.value = false
            awaitingAIResponse = false
            _errorMessage.value = "Timeout: A IA demorou muito para responder. Tente novamente."
            Log.w("ChatViewModel", "Timeout na resposta da IA")
        }
    }
}