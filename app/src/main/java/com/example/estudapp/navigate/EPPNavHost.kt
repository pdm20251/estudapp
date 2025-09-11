package com.example.estudapp.navigate

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.estudapp.ui.feature.auth.AuthViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.estudapp.ui.feature.auth.SignInScreen
import com.example.estudapp.ui.feature.auth.SignUpScreen
import com.example.estudapp.ui.feature.home.HomeScreen

@Composable
fun EPPNavHost(
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login"){
            SignInScreen(navController, authViewModel)
        }

        composable("signup"){
            SignUpScreen(navController, authViewModel)
        }

        composable("home"){
            HomeScreen(navController, authViewModel)
        }
    }
}