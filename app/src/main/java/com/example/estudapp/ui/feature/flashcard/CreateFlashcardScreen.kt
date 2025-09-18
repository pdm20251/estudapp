package com.example.estudapp.ui.feature.flashcard

import android.content.res.Resources
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import com.example.estudapp.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.estudapp.data.model.AlternativaDTO
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
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
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ){
                        Icon(Icons.Outlined.KeyboardArrowLeft, "goBack", tint = PrimaryBlue, modifier = Modifier.size(35.dp))
                    }
                },
                title = { Text("Criar novo card", color = PrimaryBlue, fontWeight = FontWeight.Black) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                //.padding(top = 20.dp, bottom = 20.dp, start = 30.dp, end = 30.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal
            ) {
                FlashcardTypeEnum.values().forEach { tabType ->
                    Tab(
                        selected = selectedTab == tabType,
                        enabled = !isEditMode,
                        onClick = { selectedTab = tabType },
                        text = {
                            Text(
                                tabType.name.replace("_", " ")
                                    .lowercase(Locale.getDefault())
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                fontSize = 12.sp,
                                lineHeight = 13.sp
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
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imagemUri = uri
        }
    val audioPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            audioUri = uri
        }

    LaunchedEffect(cardToEdit) {
        if (isEditMode && cardToEdit != null) {
            frente = cardToEdit.frente.orElseEmpty()
            verso = cardToEdit.verso.orElseEmpty()
        }
    }

    Text(
        text = "Frente (Pergunta)",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(vertical = 8.dp),
        textAlign = TextAlign.Left,
        color = PrimaryBlue
    )
    OutlinedTextField(
        value = frente,
        onValueChange = { frente = it },
        label = { Text("Pergunta") },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(30f)
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Verso (Resposta)",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(vertical = 8.dp),
        textAlign = TextAlign.Left,
        color = PrimaryBlue
    )
    OutlinedTextField(
        value = verso,
        onValueChange = { verso = it },
        label = { Text("Resposta") },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(30f)
    )

    Spacer(Modifier.height(50.dp))

    SaveButton(
        isEditMode = isEditMode,
        enabled =  saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading
    ) {
        if (isEditMode && cardToEdit != null) {
            viewModel.updateFrenteVerso(deckId, cardToEdit, frente, verso)
        } else {
            viewModel.saveFrenteVerso(deckId, frente, verso, imagemUri, audioUri)
        }
    }

    Spacer(Modifier.height(16.dp))

    GenerateCardButton(

    )

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
        placeholder = { Text("Ex: A capital do Brasil é {{c1::Brasília}}, que fica no {{c2::DF}}.") },
        shape = RoundedCornerShape(30f),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(32.dp))

    Text("Dica:\n'c1' é a resposta 1 e 'c2' é a resposta 2\ncoloque após :: o texto correto da resposta", color = LightGray, fontStyle = FontStyle.Italic, fontSize = 12.sp)

    Spacer(Modifier.height(100.dp))

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

    Spacer(Modifier.height(16.dp))

    GenerateCardButton(
        /* Inserir aqui lógica para gerar card com LLM. */
    )
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
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imagemUri = uri
        }
    val audioPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            audioUri = uri
        }

    LaunchedEffect(cardToEdit) {
        if (isEditMode && cardToEdit != null) {
            pergunta = cardToEdit.pergunta.orElseEmpty()
            respostas = cardToEdit.respostasValidas?.joinToString(", ") ?: ""
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pergunta",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                //.padding(8.dp),
        ) {
            TextField(
                value = pergunta,
                onValueChange = { pergunta = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex.: Quantos r tem em strawberry?") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(8.dp))
            if (imagemUri != null) {
                AsyncImage(
                    model = imagemUri, contentDescription = "Imagem selecionada",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
            }

            if (audioUri != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Audiotrack, contentDescription = "Áudio selecionado")
                    Spacer(Modifier.width(8.dp))
                    Text("Áudio selecionado!")
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Adicionar Imagem",
                        tint = PrimaryBlue
                    )
                }
                IconButton(onClick = { audioPicker.launch("audio/*") }) {
                    Icon(
                        Icons.Default.Audiotrack,
                        contentDescription = "Adicionar Áudio",
                        tint = PrimaryBlue
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = "Resposta",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = respostas,
            onValueChange = { respostas = it },
            label = { Text("Respostas certas") },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(30f)
        )
        Text(
            text = "Separe cada resposta por uma vírgula",
            fontSize = 12.sp
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

        Spacer(Modifier.height(16.dp))

        GenerateCardButton(
            /* Inserir aqui lógica para gerar card com LLM. */
        )
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

    val perguntaImagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            perguntaImagemUri = uri
        }
    val perguntaAudioPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            perguntaAudioUri = uri
        }

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
    Column(
        modifier = Modifier
            .fillMaxSize(),
            //.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pergunta",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        )

        OutlinedTextField(
            value = pergunta,
            onValueChange = { pergunta = it },
            //label = { Text("Texto da pergunta") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30f)
        )
        MediaSelector(
            imageUri = perguntaImagemUri,
            audioUri = perguntaAudioUri,
            onImageClick = { perguntaImagePicker.launch("image/*") },
            onAudioClick = { perguntaAudioPicker.launch("audio/*") }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Alternativas",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        )

        @Composable
        fun AlternativaInput(
            texto: String,
            onTextoChange: (String) -> Unit,
            uri: Uri?,
            onUriChange: (Uri?) -> Unit,
            label: String
        ) {
            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { newUri: Uri? ->
                    onUriChange(newUri)
                }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        color = LightGray,
                        shape = RoundedCornerShape(30f)
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = onTextoChange,
                        label = { Text(label) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(1.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { launcher.launch("image/*") }) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Adicionar Imagem à Alternativa",
                            tint = Color.Gray
                        )
                    }
                }

                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Preview da alternativa",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        AlternativaInput(alt1Texto, { alt1Texto = it }, alt1Uri, { alt1Uri = it }, "Alternativa 1")
        AlternativaInput(alt2Texto, { alt2Texto = it }, alt2Uri, { alt2Uri = it }, "Alternativa 2")
        AlternativaInput(alt3Texto, { alt3Texto = it }, alt3Uri, { alt3Uri = it }, "Alternativa 3")
        AlternativaInput(alt4Texto, { alt4Texto = it }, alt4Uri, { alt4Uri = it }, "Alternativa 4")

        Spacer(Modifier.height(16.dp))
        Text(
            "Qual é a alternativa correta?",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            (0..3).forEach { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    AlternativaDTO(
                        text = alt1Texto.ifBlank { null },
                        imageUrl = altsExistentes.getOrNull(0)?.imageUrl
                    ),
                    AlternativaDTO(
                        text = alt2Texto.ifBlank { null },
                        imageUrl = altsExistentes.getOrNull(1)?.imageUrl
                    ),
                    AlternativaDTO(
                        text = alt3Texto.ifBlank { null },
                        imageUrl = altsExistentes.getOrNull(2)?.imageUrl
                    ),
                    AlternativaDTO(
                        text = alt4Texto.ifBlank { null },
                        imageUrl = altsExistentes.getOrNull(3)?.imageUrl
                    )
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
        Spacer(modifier = Modifier.height(16.dp))
        GenerateCardButton(
            /* Inserir aqui lógica para gerar card com LLM. */
        )
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
            Icon(Icons.Default.Audiotrack, contentDescription = "Áudio selecionado", tint = PrimaryBlue)
            Text("Áudio selecionado!")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = onImageClick) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "Adicionar Imagem", tint = PrimaryBlue)
        }
        IconButton(onClick = onAudioClick) {
            Icon(Icons.Default.Audiotrack, contentDescription = "Adicionar Áudio", tint = PrimaryBlue)
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
            .fillMaxWidth()
            .height(60.dp),
        enabled = enabled,
        shape = RoundedCornerShape(30f),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondary
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                if (isEditMode) "Atualizar" else "Salvar",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            )
        }
    }
}


@Composable
private fun GenerateCardButton(){
    Button(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(30f),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),

        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gerar card com MonitorIA",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.icon_generate),
                contentDescription = "Gerar pergunta com IA"
            )
        }
    }
}


/* Extensãozinha para evitar null/blank repetido */
private fun String?.orElseEmpty(): String = this ?: ""