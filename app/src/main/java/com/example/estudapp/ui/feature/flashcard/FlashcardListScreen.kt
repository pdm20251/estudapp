package com.example.estudapp.ui.feature.flashcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardListScreen(
    navController: NavHostController,
    flashcardViewModel: FlashcardViewModel = viewModel(),
    deckId: String
) {
    LaunchedEffect(deckId) {
        flashcardViewModel.loadFlashcards(deckId)
    }

    val uiState by flashcardViewModel.flashcardsState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Flashcards do Deck") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create_flashcard/$deckId") }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Flashcard")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is FlashcardsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is FlashcardsUiState.Error -> Text(state.message, modifier = Modifier.align(Alignment.Center))
                is FlashcardsUiState.Success -> {
                    if (state.flashcards.isEmpty()) {
                        Text(
                            text = "Nenhum flashcard neste deck. Clique no '+' para começar!",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.flashcards) { flashcard ->
                                FlashcardItem(
                                    flashcard = flashcard,
                                    onDeleteClick = {
                                        flashcardViewModel.deleteFlashcard(deckId, flashcard.id)
                                    },
                                    onEditClick = {
                                        // Navega para a tela de edição passando os IDs
                                        navController.navigate("create_flashcard/${deckId}?flashcardId=${flashcard.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable // A anotação @Composable é essencial aqui
fun FlashcardItem(
    flashcard: FlashcardDTO,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit // Parâmetro para a ação de editar
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = flashcard.type.replace("_", " ").lowercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                val title = when (flashcard.type) {
                    FlashcardTypeEnum.FRENTE_VERSO.name -> flashcard.frente
                    FlashcardTypeEnum.CLOZE.name -> flashcard.textoComLacunas?.replace(Regex("\\{\\{(c\\d+)::.*?\\}\\}"), "{{....}}")
                    else -> flashcard.pergunta
                }
                Text(text = title ?: "Flashcard inválido", style = MaterialTheme.typography.bodyLarge)
            }
            // Botões de Ação
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Flashcard")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Deletar Flashcard")
                }
            }
        }
    }
}