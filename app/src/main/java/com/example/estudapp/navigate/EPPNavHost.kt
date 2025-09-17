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
import com.example.estudapp.ui.feature.flashcard.StudyScreen
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
        composable("deck_list") {
            DeckListScreen(navController)
        }
        composable("create_deck") {
            CreateDeckScreen(navController)
        }

        composable(
            route = "flashcard_list/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            if (deckId != null) {
                FlashcardListScreen(navController = navController, deckId = deckId)
            }
        }

        // --- ESTA É A ROTA CORRIGIDA ---
        composable(
            route = "create_flashcard/{deckId}?flashcardId={flashcardId}",
            arguments = listOf(
                navArgument("deckId") { type = NavType.StringType },
                navArgument("flashcardId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            val flashcardId = backStackEntry.arguments?.getString("flashcardId")
            if (deckId != null) {
                CreateFlashcardScreen(
                    navController = navController,
                    deckId = deckId,
                    flashcardId = flashcardId // Agora esta chamada está correta
                )
            }
        }

        composable(
            route = "study_session/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            if (deckId != null) {
                StudyScreen(navController = navController, deckId = deckId)
            }
        }
    }
}