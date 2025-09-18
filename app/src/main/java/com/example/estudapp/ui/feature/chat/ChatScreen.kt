package com.example.estudapp.ui.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.estudapp.data.model.SimpleChatMessageDTO
import com.example.estudapp.ui.theme.Black
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
import com.example.estudapp.ui.theme.White
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val errorMessage by chatViewModel.errorMessage.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Rastreia o último tamanho conhecido para scroll inteligente
    var lastMessageCount by remember { mutableIntStateOf(0) }
    var wasLoading by remember { mutableStateOf(false) }

    // Scroll inteligente que evita glitches na transição
    LaunchedEffect(messages.size, isLoading) {
        val currentMessageCount = messages.size

        // Casos onde devemos fazer scroll:
        // 1. Nova mensagem foi adicionada (não apenas mudança de loading)
        // 2. Loading acabou de parar (transição completa)
        when {
            // Nova mensagem real foi adicionada
            currentMessageCount > lastMessageCount && currentMessageCount > 0 -> {
                // Delay pequeno para garantir que o item foi renderizado
                kotlinx.coroutines.delay(50)
                listState.animateScrollToItem(currentMessageCount - 1)
                lastMessageCount = currentMessageCount
            }

            // Loading acabou de parar e havia mensagens
            wasLoading && !isLoading && currentMessageCount > 0 -> {
                // Scroll suave para a última mensagem após loading parar
                kotlinx.coroutines.delay(100)
                listState.animateScrollToItem(currentMessageCount - 1)
            }
        }

        // Atualiza estado anterior
        wasLoading = isLoading
    }

    // Timeout de segurança para evitar loading infinito (2 minutos)
    LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(120000) // 2 minutos
            if (isLoading) {
                chatViewModel.handleResponseTimeout()
            }
        }
    }

    // Mostra erro se houver
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Aqui você pode mostrar um snackbar ou toast se necessário
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.Outlined.KeyboardArrowLeft,
                            "goBack",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                },
                title = {
                    Text(
                        "MonitorIA",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Black
                    )
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Lista de mensagens
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (messages.isEmpty() && !isLoading) {
                    item {
                        WelcomeMessage()
                    }
                }

                items(messages) { message ->
                    ChatMessageItem(message = message)
                }

                if (isLoading) {
                    item {
                        LoadingMessage()
                    }
                }
            }

            // Campo de input
            ChatInputField(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.trim().isNotEmpty() && !isLoading) {
                        chatViewModel.sendMessage(messageText.trim())
                        messageText = ""
                        keyboardController?.hide()
                    }
                },
                isEnabled = !isLoading,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun WelcomeMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Olá! 👋",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sou a MonitorIA, sua assistente de estudos.\nEm que posso te ajudar hoje?",
            fontSize = 16.sp,
            color = Black,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ChatMessageItem(message: SimpleChatMessageDTO) {
    val isUser = message.sender == "USER"
    val timestamp = message.timestamp as? Long ?: 0L
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = if (timestamp > 0) timeFormat.format(Date(timestamp)) else ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) PrimaryBlue else LightGray
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text ?: "",
                    color = if (isUser) White else Black,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )
            }

            if (formattedTime.isNotEmpty()) {
                Text(
                    text = formattedTime,
                    color = Black.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun LoadingMessage() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(LightGray)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = PrimaryBlue,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "MonitorIA está digitando...",
                    color = Black,
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputField(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Digite sua pergunta...",
                    color = PrimaryBlue.copy(alpha = 0.6f)
                )
            },
            enabled = isEnabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.6f),
                cursorColor = PrimaryBlue,
                focusedPlaceholderColor = PrimaryBlue.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = PrimaryBlue.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (isEnabled) onSendMessage()
                }
            ),
            maxLines = 4
        )

        IconButton(
            onClick = onSendMessage,
            enabled = isEnabled && messageText.trim().isNotEmpty(),
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isEnabled && messageText.trim().isNotEmpty())
                        PrimaryBlue
                    else
                        PrimaryBlue.copy(alpha = 0.3f)
                )
                .size(48.dp)
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "Enviar mensagem",
                tint = White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}