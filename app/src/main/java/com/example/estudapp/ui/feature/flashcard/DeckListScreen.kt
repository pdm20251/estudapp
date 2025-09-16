package com.example.estudapp.ui.feature.flashcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.estudapp.data.model.DeckDTO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    navController: NavHostController,
    deckViewModel: DeckViewModel = viewModel()
) {
    val uiState by deckViewModel.decksState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Meus Decks") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create_deck") }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Deck")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is DecksUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is DecksUiState.Error -> Text(state.message, modifier = Modifier.align(Alignment.Center))
                is DecksUiState.Success -> {
                    if (state.decks.isEmpty()) {
                        Text("Nenhum deck criado. Clique no '+' para começar!", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.decks) { deck ->
                                DeckItem(deck = deck, onClick = {
                                    // Navega para a lista de flashcards DAQUELE deck
                                    navController.navigate("flashcard_list/${deck.id}")
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeckItem(deck: DeckDTO, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = deck.name, style = MaterialTheme.typography.titleMedium)
            // Usando let para mostrar a descrição apenas se ela não for nula ou vazia
            deck.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}