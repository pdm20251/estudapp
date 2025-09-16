package com.example.estudapp.ui.feature.flashcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    deckId: String // 1. ADDED: The screen now requires a deckId
) {
    // 2. ADDED: This block runs when the screen is shown, telling the ViewModel to load the cards for THIS deck.
    LaunchedEffect(deckId) {
        flashcardViewModel.loadFlashcards(deckId)
    }

    val uiState by flashcardViewModel.flashcardsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Flashcards do Deck") }) // Title updated for clarity
        },
        floatingActionButton = {
            // 3. UPDATED: The button now passes the deckId to the creation screen.
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
                is FlashcardsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FlashcardsUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
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
                                FlashcardItem(flashcard = flashcard)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardItem(flashcard: FlashcardDTO) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = flashcard.type.replace("_", " ").lowercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            val title = when (flashcard.type) {
                FlashcardTypeEnum.FRENTE_VERSO.name -> flashcard.frente
                else -> flashcard.pergunta
            }
            Text(text = title ?: "Flashcard inválido", style = MaterialTheme.typography.bodyLarge)
        }
    }
}