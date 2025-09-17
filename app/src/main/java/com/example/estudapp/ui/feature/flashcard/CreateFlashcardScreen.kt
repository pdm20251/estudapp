package com.example.estudapp.ui.feature.flashcard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
fun CreateFlashcardScreen(
    navController: NavHostController,
    flashcardViewModel: FlashcardViewModel = viewModel(),
    deckId: String,
    flashcardId: String?
) {
    val isEditMode = flashcardId != null
    val saveStatus by flashcardViewModel.saveStatus.collectAsState()
    val cardToEdit by flashcardViewModel.cardToEdit.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(FlashcardTypeEnum.FRENTE_VERSO) }

    // Carrega o card quando estiver editando
    LaunchedEffect(flashcardId) {
        if (isEditMode) {
            flashcardViewModel.loadFlashcardForEditing(deckId, flashcardId!!)
        } else {
            flashcardViewModel.clearCardToEdit()
        }
    }

    // Ajusta a aba para o tipo do card em edição
    LaunchedEffect(cardToEdit) {
        val current = cardToEdit   // <- cópia local (não-delegada)
        if (isEditMode && current != null) {
            val typeEnum = runCatching { FlashcardTypeEnum.valueOf(current.type) }
                .getOrElse { FlashcardTypeEnum.FRENTE_VERSO }
            selectedTab = typeEnum
        }
    }

    // Feedback (salvou/erro)
    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is SaveStatus.Success -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                flashcardViewModel.resetSaveStatus()
                navController.popBackStack()
            }
            is SaveStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                flashcardViewModel.resetSaveStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEditMode) "Editar Flashcard" else "Criar Novo Flashcard") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mesma UI – apenas desabilita troca de abas no modo edição
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                FlashcardTypeEnum.values().forEach { tabType ->
                    Tab(
                        selected = selectedTab == tabType,
                        enabled = !isEditMode,
                        onClick = { selectedTab = tabType },
                        text = {
                            Text(
                                tabType.name.replace("_", " ")
                                    .lowercase(Locale.getDefault())
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditMode && cardToEdit == null) {
                    CircularProgressIndicator()
                } else {
                    when (selectedTab) {
                        FlashcardTypeEnum.FRENTE_VERSO -> FormFrenteVerso(
                            deckId, flashcardViewModel, saveStatus, isEditMode, cardToEdit
                        )
                        FlashcardTypeEnum.CLOZE -> FormCloze(
                            deckId, flashcardViewModel, saveStatus, isEditMode, cardToEdit
                        )
                        FlashcardTypeEnum.DIGITE_RESPOSTA -> FormDigiteResposta(
                            deckId, flashcardViewModel, saveStatus, isEditMode, cardToEdit
                        )
                        FlashcardTypeEnum.MULTIPLA_ESCOLHA -> FormMultiplaEscolha(
                            deckId, flashcardViewModel, saveStatus, isEditMode, cardToEdit
                        )
                    }
                }
            }
        }
    }
}

/* ======================= FORMULÁRIOS ======================= */

@Composable
private fun FormFrenteVerso(
    deckId: String,
    viewModel: FlashcardViewModel,
    saveStatus: SaveStatus,
    isEditMode: Boolean,
    cardToEdit: FlashcardDTO?
) {
    var frente by remember { mutableStateOf("") }
    var verso by remember { mutableStateOf("") }

    // Se quiser usar mídia na criação, mantém aqui:
    var imagemUri by remember { mutableStateOf<Uri?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> imagemUri = uri }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> audioUri = uri }

    LaunchedEffect(cardToEdit) {
        if (isEditMode && cardToEdit != null) {
            frente = cardToEdit.frente.orElseEmpty()
            verso  = cardToEdit.verso.orElseEmpty()
        }
    }

    OutlinedTextField(
        value = frente,
        onValueChange = { frente = it },
        label = { Text("Frente (Pergunta)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = verso,
        onValueChange = { verso = it },
        label = { Text("Verso (Resposta)") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(32.dp))

    SaveButton(
        isEditMode = isEditMode,
        enabled = frente.isNotBlank() && verso.isNotBlank() && saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading
    ) {
        if (isEditMode && cardToEdit != null) {
            viewModel.updateFrenteVerso(deckId, cardToEdit, frente, verso)
        } else {
            viewModel.saveFrenteVerso(deckId, frente, verso, imagemUri, audioUri)
        }
    }
}

@Composable
private fun FormCloze(
    deckId: String,
    viewModel: FlashcardViewModel,
    saveStatus: SaveStatus,
    isEditMode: Boolean,
    cardToEdit: FlashcardDTO?
) {
    var texto by remember { mutableStateOf("") }

    LaunchedEffect(cardToEdit) {
        if (isEditMode && cardToEdit != null) {
            texto = cardToEdit.textoComLacunas.orElseEmpty()
        }
    }

    OutlinedTextField(
        value = texto,
        onValueChange = { texto = it },
        label = { Text("Texto com respostas") },
        placeholder = { Text("Ex: A capital do {{c1::Brasil}} é {{c2::Brasília}}.") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(32.dp))

    SaveButton(
        isEditMode = isEditMode,
        enabled = texto.isNotBlank() && saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading
    ) {
        val regex = Regex("\\{\\{(c\\d+)::(.*?)\\}\\}")
        val respostasMap = regex.findAll(texto).associate { it.groupValues[1] to it.groupValues[2] }

        if (isEditMode && cardToEdit != null) {
            viewModel.updateCloze(deckId, cardToEdit, texto, respostasMap)
        } else {
            viewModel.saveCloze(deckId, texto, respostasMap)
        }
    }
}

@Composable
private fun FormDigiteResposta(
    deckId: String,
    viewModel: FlashcardViewModel,
    saveStatus: SaveStatus,
    isEditMode: Boolean,
    cardToEdit: FlashcardDTO?
) {
    var pergunta by remember { mutableStateOf("") }
    var respostas by remember { mutableStateOf("") }

    var imagemUri by remember { mutableStateOf<Uri?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> imagemUri = uri }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> audioUri = uri }

    LaunchedEffect(cardToEdit) {
        if (isEditMode && cardToEdit != null) {
            pergunta = cardToEdit.pergunta.orElseEmpty()
            respostas = cardToEdit.respostasValidas?.joinToString(", ") ?: ""
        }
    }

    OutlinedTextField(
        value = pergunta,
        onValueChange = { pergunta = it },
        label = { Text("Pergunta") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    // Pré-visualização de mídia (apenas se selecionar nova na criação)
    if (imagemUri != null) {
        AsyncImage(
            model = imagemUri, contentDescription = "Imagem selecionada",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(8.dp))
    }
    if (audioUri != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Audiotrack, contentDescription = "Áudio selecionado")
            Spacer(Modifier.width(8.dp))
            Text("Áudio selecionado!")
        }
        Spacer(Modifier.height(8.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = { imagePicker.launch("image/*") }) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "Adicionar Imagem")
        }
        IconButton(onClick = { audioPicker.launch("audio/*") }) {
            Icon(Icons.Default.Audiotrack, contentDescription = "Adicionar Áudio")
        }
    }

    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = respostas,
        onValueChange = { respostas = it },
        label = { Text("Respostas válidas (separadas por vírgula)") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(32.dp))

    SaveButton(
        isEditMode = isEditMode,
        enabled = pergunta.isNotBlank() && respostas.isNotBlank() && saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading
    ) {
        val respostasList = respostas.split(',').map { it.trim() }.filter { it.isNotBlank() }
        if (isEditMode && cardToEdit != null) {
            viewModel.updateDigiteResposta(deckId, cardToEdit, pergunta, respostasList)
        } else {
            viewModel.saveDigiteResposta(deckId, pergunta, respostasList, imagemUri, audioUri)
        }
    }
}

@Composable
private fun FormMultiplaEscolha(
    deckId: String,
    viewModel: FlashcardViewModel,
    saveStatus: SaveStatus,
    isEditMode: Boolean,
    cardToEdit: FlashcardDTO?
) {
    var pergunta by remember { mutableStateOf("") }
    var perguntaImagemUri by remember { mutableStateOf<Uri?>(null) }
    var perguntaAudioUri by remember { mutableStateOf<Uri?>(null) }

    var alt1Texto by remember { mutableStateOf("") }
    var alt1Uri by remember { mutableStateOf<Uri?>(null) }
    var alt2Texto by remember { mutableStateOf("") }
    var alt2Uri by remember { mutableStateOf<Uri?>(null) }
    var alt3Texto by remember { mutableStateOf("") }
    var alt3Uri by remember { mutableStateOf<Uri?>(null) }
    var alt4Texto by remember { mutableStateOf("") }
    var alt4Uri by remember { mutableStateOf<Uri?>(null) }
    var respostaCorretaIndex by remember { mutableIntStateOf(0) }

    val perguntaImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> perguntaImagemUri = uri }
    val perguntaAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> perguntaAudioUri = uri }

    // Preenche campos no modo edição
    LaunchedEffect(cardToEdit) {
        if (isEditMode && cardToEdit != null) {
            pergunta = cardToEdit.pergunta.orElseEmpty()
            val alts = cardToEdit.alternativas ?: emptyList()
            alt1Texto = alts.getOrNull(0)?.text.orElseEmpty()
            alt2Texto = alts.getOrNull(1)?.text.orElseEmpty()
            alt3Texto = alts.getOrNull(2)?.text.orElseEmpty()
            alt4Texto = alts.getOrNull(3)?.text.orElseEmpty()
            val correta = cardToEdit.respostaCorreta
            respostaCorretaIndex = alts.indexOfFirst { it == correta }.takeIf { it >= 0 } ?: 0
        }
    }

    Text("Pergunta", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = pergunta,
        onValueChange = { pergunta = it },
        label = { Text("Texto da pergunta (opcional se tiver imagem)") },
        modifier = Modifier.fillMaxWidth()
    )
    MediaSelector(
        imageUri = perguntaImagemUri,
        audioUri = perguntaAudioUri,
        onImageClick = { perguntaImagePicker.launch("image/*") },
        onAudioClick = { perguntaAudioPicker.launch("audio/*") }
    )

    Spacer(Modifier.height(24.dp))

    Text("Alternativas", style = MaterialTheme.typography.titleMedium)

    @Composable
    fun AlternativaInput(
        texto: String,
        onTextoChange: (String) -> Unit,
        uri: Uri?,
        onUriChange: (Uri?) -> Unit,
        label: String
    ) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { newUri: Uri? -> onUriChange(newUri) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = texto,
                onValueChange = onTextoChange,
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { launcher.launch("image/*") }) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Adicionar Imagem à Alternativa")
            }
        }
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = "Preview da alternativa",
                modifier = Modifier
                    .height(60.dp)
                    .padding(start = 16.dp)
            )
        }
    }

    AlternativaInput(alt1Texto, { alt1Texto = it }, alt1Uri, { alt1Uri = it }, "Alternativa 1")
    AlternativaInput(alt2Texto, { alt2Texto = it }, alt2Uri, { alt2Uri = it }, "Alternativa 2")
    AlternativaInput(alt3Texto, { alt3Texto = it }, alt3Uri, { alt3Uri = it }, "Alternativa 3")
    AlternativaInput(alt4Texto, { alt4Texto = it }, alt4Uri, { alt4Uri = it }, "Alternativa 4")

    Spacer(Modifier.height(16.dp))
    Text("Qual é a alternativa correta?")
    Column {
        (0..3).forEach { index ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = respostaCorretaIndex == index,
                    onClick = { respostaCorretaIndex = index }
                )
                Text("Alternativa ${index + 1}")
            }
        }
    }

    Spacer(Modifier.height(32.dp))

    SaveButton(
        isEditMode = isEditMode,
        enabled = saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading
    ) {
        if (isEditMode && cardToEdit != null) {
            // Mantém as imageUrl atuais das alternativas (se existirem)
            val altsExistentes = cardToEdit.alternativas ?: emptyList()
            val alternativasAtualizadas = listOf(
                AlternativaDTO(text = alt1Texto.ifBlank { null }, imageUrl = altsExistentes.getOrNull(0)?.imageUrl),
                AlternativaDTO(text = alt2Texto.ifBlank { null }, imageUrl = altsExistentes.getOrNull(1)?.imageUrl),
                AlternativaDTO(text = alt3Texto.ifBlank { null }, imageUrl = altsExistentes.getOrNull(2)?.imageUrl),
                AlternativaDTO(text = alt4Texto.ifBlank { null }, imageUrl = altsExistentes.getOrNull(3)?.imageUrl)
            )

            viewModel.updateMultiplaEscolha(
                deckId = deckId,
                originalCard = cardToEdit,
                pergunta = pergunta,
                alternativas = alternativasAtualizadas,
                respostaCorretaIndex = respostaCorretaIndex
            )
        } else {
            // Criação: o seu ViewModel quer pares (texto, Uri?)
            val alternativasUris: List<Pair<String, Uri?>> = listOf(
                alt1Texto to alt1Uri,
                alt2Texto to alt2Uri,
                alt3Texto to alt3Uri,
                alt4Texto to alt4Uri
            )
            viewModel.saveMultiplaEscolha(
                deckId = deckId,
                pergunta = pergunta,
                perguntaImagemUri = perguntaImagemUri,
                perguntaAudioUri = perguntaAudioUri,
                alternativasUris = alternativasUris,
                respostaCorretaIndex = respostaCorretaIndex
            )
        }
    }
}

/* ======================= HELPERS ======================= */

@Composable
fun MediaSelector(
    imageUri: Uri?,
    audioUri: Uri?,
    onImageClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    if (imageUri != null) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Imagem selecionada",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
    if (audioUri != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Audiotrack, contentDescription = "Áudio selecionado")
            Text("Áudio selecionado!")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = onImageClick) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "Adicionar Imagem")
        }
        IconButton(onClick = onAudioClick) {
            Icon(Icons.Default.Audiotrack, contentDescription = "Adicionar Áudio")
        }
    }
}

@Composable
private fun SaveButton(
    isEditMode: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(55.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(if (isEditMode) "Atualizar" else "Salvar")
        }
    }
}

/* Extensãozinha para evitar null/blank repetido */
private fun String?.orElseEmpty(): String = this ?: ""