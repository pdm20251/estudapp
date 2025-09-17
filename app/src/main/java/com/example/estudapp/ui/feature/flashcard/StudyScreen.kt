package com.example.estudapp.ui.feature.flashcard

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.estudapp.data.model.AlternativaDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    navController: NavHostController,
    studyViewModel: StudyViewModel = viewModel(),
    deckId: String
) {
    LaunchedEffect(deckId) {
        studyViewModel.startStudySession(deckId)
    }

    val uiState by studyViewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sessão de Estudo") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is StudyUiState.Loading -> CircularProgressIndicator()
                is StudyUiState.Error -> Text(state.message)
                is StudyUiState.EmptyDeck -> Text("Este deck não tem cards para estudar.")
                is StudyUiState.SessionFinished -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sessão finalizada!", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Voltar para os Decks")
                        }
                    }
                }
                is StudyUiState.Studying -> {
                    StudyCard(
                        key = state.card.id,
                        state = state,
                        onCheckAnswer = { userAnswer, clozeAnswers, multipleChoiceAnswer ->
                            studyViewModel.checkAnswer(userAnswer, clozeAnswers, multipleChoiceAnswer)
                        },
                        onShowAnswer = { studyViewModel.showAnswer() }, // Passa a nova função
                        onNextCard = { studyViewModel.nextCard() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyCard(
    key: String,
    state: StudyUiState.Studying,
    onCheckAnswer: (String, Map<String, String>, AlternativaDTO?) -> Unit,
    onShowAnswer: () -> Unit, // Recebe a nova função
    onNextCard: () -> Unit
) {
    var userAnswer by remember(key) { mutableStateOf("") }
    var selectedOption by remember(key) { mutableStateOf<AlternativaDTO?>(null) }
    var clozeAnswers by remember(key) { mutableStateOf(mapOf<String, String>()) }
    val cardType = state.card.type

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (cardType) {
                FlashcardTypeEnum.FRENTE_VERSO.name -> {
                    // Agora não precisa mais passar os parâmetros de resposta do usuário
                    FrenteVersoStudy(state = state)
                }
                FlashcardTypeEnum.MULTIPLA_ESCOLHA.name -> {
                    MultiplaEscolhaStudy(state = state, selectedOption = selectedOption, onOptionSelected = { selectedOption = it })
                }
                FlashcardTypeEnum.DIGITE_RESPOSTA.name -> {
                    DigiteRespostaStudy(state = state, userAnswer = userAnswer, onUserAnswerChange = { userAnswer = it })
                }
                FlashcardTypeEnum.CLOZE.name -> {
                    ClozeStudy(
                        state = state,
                        userAnswers = clozeAnswers,
                        onUserAnswerChange = { key, value ->
                            clozeAnswers = clozeAnswers + (key to value)
                        }
                    )
                }
            }
        }

        Column {
            if (state.isShowingAnswer) {
                Button(onClick = onNextCard, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar")
                }
            } else {
                // LÓGICA DO BOTÃO ATUALIZADA
                val isRevealType = state.card.type == FlashcardTypeEnum.FRENTE_VERSO.name
                val buttonText = if (isRevealType) "Mostrar Resposta" else "Verificar Resposta"

                Button(
                    onClick = {
                        if (isRevealType) {
                            onShowAnswer() // Chama a função simples de mostrar
                        } else {
                            // Mantém a lógica de verificação para os outros
                            when (cardType) {
                                FlashcardTypeEnum.MULTIPLA_ESCOLHA.name -> onCheckAnswer("", emptyMap(), selectedOption)
                                FlashcardTypeEnum.CLOZE.name -> onCheckAnswer("", clozeAnswers, null)
                                else -> onCheckAnswer(userAnswer, emptyMap(), null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}


@Composable
fun FrenteVersoStudy(state: StudyUiState.Studying) {
    val card = state.card

    card.perguntaImageUrl?.let { imageUrl ->
        AsyncImage(
            model = imageUrl,
            contentDescription = "Imagem da pergunta",
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
    card.perguntaAudioUrl?.let { audioUrl ->
        AudioPlayer(url = audioUrl)
        Spacer(modifier = Modifier.height(16.dp))
    }

    Text(text = card.frente ?: "", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)

    // O CAMPO DE TEXTO FOI REMOVIDO DAQUI

    if (state.isShowingAnswer) {
        Spacer(modifier = Modifier.height(24.dp))
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text(text = "Resposta: ${card.verso}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun DigiteRespostaStudy(state: StudyUiState.Studying, userAnswer: String, onUserAnswerChange: (String) -> Unit) {
    val card = state.card

    card.perguntaImageUrl?.let { imageUrl ->
        AsyncImage(
            model = imageUrl,
            contentDescription = "Imagem da pergunta",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
    card.perguntaAudioUrl?.let { audioUrl ->
        AudioPlayer(url = audioUrl)
        Spacer(modifier = Modifier.height(16.dp))
    }

    Text(text = card.pergunta ?: "", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedTextField(
        value = userAnswer,
        onValueChange = onUserAnswerChange,
        label = { Text("Digite sua resposta...") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isShowingAnswer
    )
    if (state.isShowingAnswer) {
        val feedbackColor = if (state.wasCorrect == true) Color(0xFF2E7D32) else Color.Red
        val feedbackText = if (state.wasCorrect == true) "Resposta correta!" else "Resposta incorreta!"
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = feedbackText, color = feedbackColor, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Respostas esperadas: ${card.respostasValidas?.joinToString(" ou ")}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MultiplaEscolhaStudy(state: StudyUiState.Studying, selectedOption: AlternativaDTO?, onOptionSelected: (AlternativaDTO) -> Unit) {
    val card = state.card

    // Exibe a imagem ou áudio da pergunta, se existirem
    card.perguntaImageUrl?.let { imageUrl ->
        AsyncImage(
            model = imageUrl,
            contentDescription = "Imagem da pergunta",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
    card.perguntaAudioUrl?.let { audioUrl ->
        AudioPlayer(url = audioUrl)
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Exibe o texto da pergunta
    Text(text = card.pergunta ?: "", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(24.dp))

// Itera sobre as alternativas, ignorando qualquer uma que seja nula
    card.alternativas?.filterNotNull()?.forEach { alternativa ->
        val isCorrect = alternativa == card.respostaCorreta
        val buttonColors = when {
            state.isShowingAnswer && isCorrect -> ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Verde para a correta
            state.isShowingAnswer && selectedOption == alternativa && !isCorrect -> ButtonDefaults.buttonColors(containerColor = Color.Red) // Vermelho para a incorreta selecionada
            !state.isShowingAnswer && selectedOption == alternativa -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) // Cor para indicar seleção
            else -> ButtonDefaults.buttonColors() // Cor padrão
        }
        Button(
            onClick = { if (!state.isShowingAnswer) onOptionSelected(alternativa) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = buttonColors
        ) {
            // Se a alternativa tiver uma imagem, mostra a imagem. Senão, mostra o texto.
            if (alternativa.imageUrl != null) {
                AsyncImage(
                    model = alternativa.imageUrl,
                    contentDescription = "Imagem da alternativa",
                    modifier = Modifier.height(100.dp)
                )
            } else {
                Text(alternativa.text ?: "")
            }
        }
    }
}

@Composable
fun ClozeStudy(
    state: StudyUiState.Studying,
    userAnswers: Map<String, String>,
    onUserAnswerChange: (key: String, value: String) -> Unit
) {
    val card = state.card
    val regex = remember { Regex("\\{\\{(c\\d+)::.*?\\}\\}") }
    val textWithBlanks = remember(card.textoComLacunas) {
        card.textoComLacunas?.replace(regex, " {{....}} ") ?: ""
    }
    val labels = remember(card.textoComLacunas) {
        regex.findAll(card.textoComLacunas ?: "").map { it.groupValues[1] }.toList()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = textWithBlanks, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        labels.forEach { label ->
            OutlinedTextField(
                value = userAnswers[label] ?: "",
                onValueChange = { onUserAnswerChange(label, it) },
                label = { Text("Resposta para ${label.uppercase()}") },
                modifier = Modifier.padding(vertical = 8.dp),
                enabled = !state.isShowingAnswer,
                singleLine = true
            )
        }

        if (state.isShowingAnswer) {
            val feedbackColor = if (state.wasCorrect == true) Color(0xFF2E7D32) else Color.Red
            val feedbackText = if (state.wasCorrect == true) "Respostas corretas!" else "Alguma resposta está incorreta."

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = feedbackText, color = feedbackColor, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text(buildAnnotatedString {
                var lastIndex = 0
                val coloredRegex = Regex("\\{\\{(c\\d+)::(.*?)\\}\\}")
                coloredRegex.findAll(card.textoComLacunas ?: "").forEach { matchResult ->
                    val (key, value) = matchResult.destructured
                    append(card.textoComLacunas?.substring(lastIndex, matchResult.range.first))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Blue)) {
                        append(value)
                    }
                    lastIndex = matchResult.range.last + 1
                }
                if (lastIndex < (card.textoComLacunas?.length ?: 0)) {
                    append(card.textoComLacunas?.substring(lastIndex))
                }
            }, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AudioPlayer(url: String) {
    val mediaPlayer = remember { MediaPlayer() }
    DisposableEffect(url) {
        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            mediaPlayer.release()
        }
    }

    IconButton(onClick = { if (!mediaPlayer.isPlaying) mediaPlayer.start() }) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Tocar Áudio", modifier = Modifier.size(48.dp))
    }
}