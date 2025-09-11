package com.example.estudapp.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.navigation.NavHostController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.estudapp.ui.feature.auth.AuthViewModel
import com.example.estudapp.ui.theme.PrimaryBlue


@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.consumeWindowInsets(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100f))
                    .fillMaxWidth(0.8f)
                    .height(80.dp)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ){

            }

        }
    }
    
}

@Preview
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    val authViewModel = AuthViewModel()
    HomeScreen(navController = navController, authViewModel = authViewModel)
}
