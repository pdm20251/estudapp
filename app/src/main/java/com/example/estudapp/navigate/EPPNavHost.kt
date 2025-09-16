package com.example.estudapp.navigate

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.estudapp.ui.feature.auth.AuthViewModel
import com.example.estudapp.ui.feature.auth.SignInScreen
import com.example.estudapp.ui.feature.auth.SignUpScreen
import com.example.estudapp.ui.feature.flashcard.CreateDeckScreen
import com.example.estudapp.ui.feature.flashcard.CreateFlashcardScreen
import com.example.estudapp.ui.feature.flashcard.DeckListScreen
import com.example.estudapp.ui.feature.flashcard.FlashcardListScreen
import com.example.estudapp.ui.feature.home.HomeScreen

@Composable
fun EPPNavHost(
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            SignInScreen(navController, authViewModel)
        }

        composable("signup") {
            SignUpScreen(navController, authViewModel)
        }

        composable("home") {
            HomeScreen(navController, authViewModel)
        }

        // --- NOVAS ROTAS PARA DECKS ---

        composable("deck_list") {
            DeckListScreen(navController)
        }

        composable("create_deck") {
            CreateDeckScreen(navController)
        }

        // --- ROTAS ANTIGAS MODIFICADAS PARA ACEITAR O deckId ---

        composable(
            route = "flashcard_list/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            // Garante que o deckId não seja nulo antes de chamar a tela
            if (deckId != null) {
                FlashcardListScreen(navController = navController, deckId = deckId)
            }
        }

        composable(
            route = "create_flashcard/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            if (deckId != null) {
                CreateFlashcardScreen(navController = navController, deckId = deckId)
            }
        }
    }
}