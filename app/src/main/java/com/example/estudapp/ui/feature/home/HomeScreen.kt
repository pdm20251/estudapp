package com.example.estudapp.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.estudapp.ui.feature.auth.AuthState
import com.example.estudapp.ui.feature.auth.AuthViewModel
import com.example.estudapp.ui.theme.ErrorRed

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    // Observa o estado de autenticação para reagir ao logout
    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            // Se o estado mudar para não autenticado, volta para a tela de login
            navController.navigate("login") {
                // Limpa a pilha de navegação para que o usuário não possa voltar para a home
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp), // Adiciona um padding geral
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Bem-vindo!", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(32.dp))

            // Este botão agora navega para a lista de flashcards
            Button(onClick = { navController.navigate("flashcard_list") }) {
                Text("Ver Meus Flashcards")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÃO DE SAIR (LOGOUT)
            Button(
                onClick = { authViewModel.signout() }, // Chama a função de logout no ViewModel
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed) // Deixa o botão vermelho
            ) {
                Text("Sair")
            }

            Button(onClick = { navController.navigate("map") }) {
                Text("Meus Locais de Estudo")
            }

            Button(onClick = { navController.navigate("deck_list") }) {
                Text("Meus Decks")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    val authViewModel = AuthViewModel()
    HomeScreen(navController = navController, authViewModel = authViewModel)
}