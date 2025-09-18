package com.example.estudapp.ui.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.estudapp.R
import com.example.estudapp.ui.feature.auth.AuthState
import com.example.estudapp.ui.feature.auth.AuthViewModel
import com.example.estudapp.ui.theme.ErrorRed
import com.example.estudapp.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    var name by remember {
        mutableStateOf("")
    }

    name = authViewModel.user!!.displayName ?: "Erro ao carregar nome"

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

    Scaffold (
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ){
                        Icon(Icons.Outlined.KeyboardArrowLeft, "goBack", tint = PrimaryBlue, modifier = Modifier.size(35.dp))
                    }
                },
                title = { Text("Perfil", color = PrimaryBlue, fontWeight = FontWeight.Black) },
            )
        }
    ) { paddingValues ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Image(painter = painterResource(id = R.drawable.icon_profile), contentDescription = null, Modifier
                .size(120.dp))

            Spacer(Modifier.height(120.dp))

            Text(
                text = "Nome", color = PrimaryBlue,
                fontSize = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 28.dp)
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(55.dp),
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Seu nome") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = PrimaryBlue,
                    cursorColor = PrimaryBlue,
                    errorIndicatorColor = ErrorRed
                ),
                shape = RoundedCornerShape(30f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { authViewModel.changeName(name) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(30f),
                enabled = (name.length >= 3)
            ) {
                Text(text = "Mudar nome", fontSize = 18.sp)
            }

            Spacer(Modifier.height(40.dp))

            TextButton(
                onClick = { authViewModel.signout() },
                ) {
                Text("Sair", color = PrimaryBlue, fontSize = 15.sp)
            }
        }
    }
}