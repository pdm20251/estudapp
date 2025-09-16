package com.example.estudapp.ui.feature.flashcard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.estudapp.data.model.FlashcardTypeEnum
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFlashcardScreen(
    navController: NavHostController,
    flashcardViewModel: FlashcardViewModel = viewModel(),
    deckId: String // 1. ADDED: The screen now requires a deckId
) {
    val saveStatus by flashcardViewModel.saveStatus.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(FlashcardTypeEnum.FRENTE_VERSO) }

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
            TopAppBar(title = { Text("Criar Novo Flashcard") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                FlashcardTypeEnum.entries.forEach { tabType ->
                    Tab(
                        selected = selectedTab == tabType,
                        onClick = { selectedTab = tabType },
                        text = { Text(tabType.name.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) }
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
                // 2. UPDATED: Pass the deckId down to the specific form composables
                when (selectedTab) {
                    FlashcardTypeEnum.FRENTE_VERSO -> FormFrenteVerso(deckId, flashcardViewModel, saveStatus)
                    FlashcardTypeEnum.CLOZE -> FormCloze(deckId, flashcardViewModel, saveStatus)
                    FlashcardTypeEnum.DIGITE_RESPOSTA -> FormDigiteResposta(deckId, flashcardViewModel, saveStatus)
                    FlashcardTypeEnum.MULTIPLA_ESCOLHA -> FormMultiplaEscolha(deckId, flashcardViewModel, saveStatus)
                }
            }
        }
    }
}


@Composable
private fun FormFrenteVerso(deckId: String, viewModel: FlashcardViewModel, saveStatus: SaveStatus) {
    var frente by remember { mutableStateOf("") }
    var verso by remember { mutableStateOf("") }

    OutlinedTextField(value = frente, onValueChange = { frente = it }, label = { Text("Frente") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = verso, onValueChange = { verso = it }, label = { Text("Verso") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(32.dp))
    SaveButton(
        enabled = frente.isNotBlank() && verso.isNotBlank() && saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading,
        // 3. UPDATED: Pass the deckId to the ViewModel
        onClick = { viewModel.saveFrenteVerso(deckId, frente, verso) }
    )
}

@Composable
private fun FormCloze(deckId: String, viewModel: FlashcardViewModel, saveStatus: SaveStatus) {
    var texto by remember { mutableStateOf("") }
    var respostas by remember { mutableStateOf("") }

    OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Texto com lacunas") }, placeholder = { Text("Ex: O [c1::Brasil] é um país.") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = respostas, onValueChange = { respostas = it }, label = { Text("Respostas das lacunas") }, placeholder = { Text("Ex: c1=Brasil,c2=país") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(32.dp))
    SaveButton(
        enabled = texto.isNotBlank() && respostas.isNotBlank() && saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading,
        onClick = {
            val respostasMap = respostas.split(',').associate {
                val (key, value) = it.split('=', limit = 2)
                key.trim() to value.trim()
            }
            viewModel.saveCloze(deckId, texto, respostasMap)
        }
    )
}

@Composable
private fun FormDigiteResposta(deckId: String, viewModel: FlashcardViewModel, saveStatus: SaveStatus) {
    var pergunta by remember { mutableStateOf("") }
    var respostas by remember { mutableStateOf("") }

    OutlinedTextField(value = pergunta, onValueChange = { pergunta = it }, label = { Text("Pergunta") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = respostas, onValueChange = { respostas = it }, label = { Text("Respostas válidas (separadas por vírgula)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(32.dp))
    SaveButton(
        enabled = pergunta.isNotBlank() && respostas.isNotBlank() && saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading,
        onClick = {
            val respostasList = respostas.split(',').map { it.trim() }
            viewModel.saveDigiteResposta(deckId, pergunta, respostasList)
        }
    )
}

@Composable
private fun FormMultiplaEscolha(deckId: String, viewModel: FlashcardViewModel, saveStatus: SaveStatus) {
    var pergunta by remember { mutableStateOf("") }
    var alternativa1 by remember { mutableStateOf("") }
    var alternativa2 by remember { mutableStateOf("") }
    var alternativa3 by remember { mutableStateOf("") }
    var alternativa4 by remember { mutableStateOf("") }
    var respostaCorretaIndex by remember { mutableIntStateOf(0) }

    val alternativas = listOf(alternativa1, alternativa2, alternativa3, alternativa4)

    OutlinedTextField(value = pergunta, onValueChange = { pergunta = it }, label = { Text("Pergunta") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = alternativa1, onValueChange = { alternativa1 = it }, label = { Text("Alternativa 1") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = alternativa2, onValueChange = { alternativa2 = it }, label = { Text("Alternativa 2") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = alternativa3, onValueChange = { alternativa3 = it }, label = { Text("Alternativa 3") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = alternativa4, onValueChange = { alternativa4 = it }, label = { Text("Alternativa 4") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    Text("Qual é a alternativa correta?")
    Column {
        alternativas.forEachIndexed { index, texto ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(selected = respostaCorretaIndex == index, onClick = { respostaCorretaIndex = index })
                Text(text = texto.ifBlank { "Alternativa ${index + 1}" })
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    SaveButton(
        enabled = pergunta.isNotBlank() && alternativas.all { it.isNotBlank() } && saveStatus !is SaveStatus.Loading,
        isLoading = saveStatus is SaveStatus.Loading,
        onClick = {
            viewModel.saveMultiplaEscolha(deckId, pergunta, alternativas, alternativas[respostaCorretaIndex])
        }
    )
}

@Composable
private fun SaveButton(enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(55.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text("Salvar")
        }
    }
}