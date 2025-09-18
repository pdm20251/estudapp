package com.example.estudapp.ui.feature.flashcard

import android.Manifest
import android.hardware.lights.Light
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.estudapp.data.model.AlternativaDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.ui.feature.location.LocationViewModel
import com.example.estudapp.ui.theme.ErrorRed
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
import com.example.estudapp.ui.theme.White
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale
import kotlin.text.set

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StudyScreen(
    navController: NavHostController,
    studyViewModel: StudyViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    deckId: String,
    deckName: String
) {
    val scoreValues by studyViewModel.scoreValues.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(deckId) {
        val latlng = locationViewModel.fetchCurrentLocationAnonymously(context)
        studyViewModel.startStudySession(deckId, latlng)
    }

//    LaunchedEffect(studyViewModel.currentSessionTotalScore) {
//        val totalReceivedScore = studyViewModel.currentSessionTotalScore
//        totalScore = totalReceivedScore ?: 0.0
//    }
//
//    LaunchedEffect(studyViewModel.currentSessionPossibleScore) {
//        val possibleReceivedScore = studyViewModel.currentSessionPossibleScore
//        possibleScore = possibleReceivedScore ?: 0.0
//    }

    val uiState by studyViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigate("home") }
                    ){
                        Icon(Icons.Outlined.KeyboardArrowLeft, "goBack", tint = PrimaryBlue, modifier = Modifier.size(35.dp))
                    }
                },
                title = { Text("Estudar", color = PrimaryBlue, fontWeight = FontWeight.Black) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(deckName ?: "Error", fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))

            when (val state = uiState) {
                is StudyUiState.Loading -> CircularProgressIndicator()
                is StudyUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30f))
                            .background(LightGray)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        Icon(Icons.Outlined.Warning, contentDescription = "warning", tint = PrimaryBlue)
                        Text(state.message, color = PrimaryBlue, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }
                }
                is StudyUiState.EmptyDeck -> {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30f))
                            .background(LightGray)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        Icon(Icons.Outlined.Info, contentDescription = "info", tint = PrimaryBlue)
                        Text("Este deck não\ntem cards para estudar.", color = PrimaryBlue, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }
                }
                is StudyUiState.SessionFinished -> {

                    Column(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = LightGray,
                                shape = RoundedCornerShape(30f)
                            )
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🎉", textAlign = TextAlign.Center, fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))

                        Text("Parabéns!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        Text("Você finalizou todos os cards de $deckName,\ncontinue estudando!", textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))

                        Text("${scoreValues[0].toString()}/${scoreValues[1].toString()}", fontWeight = FontWeight.Bold)
                        Text("pontos")
                    }

                    Row (
                        modifier = Modifier
                            .clip(RoundedCornerShape(30f))
                            .background(PrimaryBlue)
                            .padding(13.dp)
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable(
                                onClick = { navController.navigate("deck_list") }
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = White, modifier = Modifier.size(40.dp))

                        Spacer(Modifier.width(15.dp))

                        Text(text = "Voltar para meus decks", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = White)
                    }
                    Spacer(Modifier.height(130.dp))
                }
                is StudyUiState.Studying -> {
                    StudyCard(
                        key = state.card.id,
                        state = state,
                        onCheckAnswer = { userAnswer, clozeAnswers, multipleChoiceAnswer ->
                            studyViewModel.checkAnswer(userAnswer, clozeAnswers, multipleChoiceAnswer)
                        },
                        onShowAnswer = { studyViewModel.showAnswer() },
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
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = LightGray,
                    shape = RoundedCornerShape(30f)
                )
                .fillMaxHeight(0.5f)
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            when (cardType) {
                FlashcardTypeEnum.FRENTE_VERSO.name -> {
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

        Spacer(Modifier.height(80.dp))

        Column {
            if (state.isShowingAnswer) {
                Button(
                    onClick = onNextCard,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(30f)
                ) {
                    Text(text = "Continuar", fontSize = 18.sp)
                }
            } else {
                val isRevealType = state.card.type == FlashcardTypeEnum.FRENTE_VERSO.name
                val buttonText = if (isRevealType) "Mostrar resposta" else "Verificar resposta"

                Button(
                    onClick = {
                        if (isRevealType) {
                            onShowAnswer()
                        } else {
                            when (cardType) {
                                FlashcardTypeEnum.MULTIPLA_ESCOLHA.name -> onCheckAnswer("", emptyMap(), selectedOption)
                                FlashcardTypeEnum.CLOZE.name -> onCheckAnswer("", clozeAnswers, null)
                                else -> onCheckAnswer(userAnswer, emptyMap(), null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(30f)
                ) {
                    Text(buttonText, color = White, fontWeight = FontWeight.Bold)
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

    if (state.isShowingAnswer) {
        //Spacer(modifier = Modifier.height(16.dp))
        Divider(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(0.9f), thickness = 1.dp, color = LightGray)
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
    //Spacer(modifier = Modifier.height(32.dp))
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(55.dp),
        enabled = !state.isShowingAnswer,
        value = userAnswer,
        onValueChange = onUserAnswerChange,
        placeholder = { Text("Digite sua resposta...") },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = LightGray,
            unfocusedIndicatorColor = LightGray,
            cursorColor = PrimaryBlue,
            errorIndicatorColor = ErrorRed,
            unfocusedPlaceholderColor = PrimaryBlue,
            focusedPlaceholderColor = PrimaryBlue
        ),
        shape = RoundedCornerShape(30f)
    )
    if (state.isShowingAnswer) {
        val feedbackColor = if (state.wasCorrect == true) Color(0xFF2E7D32) else Color.Red
        val feedbackText = if (state.wasCorrect == true) "Resposta correta!" else "Resposta incorreta!"
        //Spacer(modifier = Modifier.height(24.dp))
        Text(text = feedbackText, color = feedbackColor, style = MaterialTheme.typography.titleMedium)
        //Spacer(modifier = Modifier.height(8.dp))
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
    Spacer(modifier = Modifier.height(18.dp))

// Itera sobre as alternativas, ignorando qualquer uma que seja nula
    card.alternativas?.filterNotNull()?.forEach { alternativa ->
        val isCorrect = alternativa == card.respostaCorreta
        val buttonColors = when {
            state.isShowingAnswer && isCorrect -> ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Verde para a correta
            state.isShowingAnswer && selectedOption == alternativa && !isCorrect -> ButtonDefaults.buttonColors(containerColor = Color.Red) // Vermelho para a incorreta selecionada
            !state.isShowingAnswer && selectedOption == alternativa -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) // Cor para indicar seleção
            else -> ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        }
        Button(
            onClick = { if (!state.isShowingAnswer) onOptionSelected(alternativa) },
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(0.85f),
            colors = buttonColors,
            shape = RoundedCornerShape(30f)
        ) {
            if (alternativa.imageUrl != null) {
                AsyncImage(
                    model = alternativa.imageUrl,
                    contentDescription = "Imagem da alternativa",
                    modifier = Modifier.height(100.dp)
                )
            } else {
                Text(alternativa.text ?: "", color = White)
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
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 8.dp),
                enabled = !state.isShowingAnswer,
                value = userAnswers[label] ?: "",
                onValueChange = { onUserAnswerChange(label, it) },
                singleLine = true,
                placeholder = { Text("Resposta para ${label.uppercase()}") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = LightGray,
                    unfocusedIndicatorColor = LightGray,
                    cursorColor = PrimaryBlue,
                    errorIndicatorColor = ErrorRed,
                    unfocusedPlaceholderColor = PrimaryBlue,
                    focusedPlaceholderColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(30f)
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
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryBlue)) {
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
        Icon(Icons.Default.PlayArrow, contentDescription = "Tocar Áudio", modifier = Modifier.size(48.dp), tint = PrimaryBlue)
    }
}