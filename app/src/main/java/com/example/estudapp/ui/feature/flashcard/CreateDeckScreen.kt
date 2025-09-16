// Crie este novo arquivo em ui/feature/flashcard/CreateDeckScreen.kt
package com.example.estudapp.ui.feature.flashcard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeckScreen(
    navController: NavHostController,
    deckViewModel: DeckViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Criar Novo Deck") }) }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Deck") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição (Opcional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    deckViewModel.createDeck(name, description)
                    Toast.makeText(context, "Deck '$name' criado!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            ) {
                Text("Salvar Deck")
            }
        }
    }
}