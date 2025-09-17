package com.example.estudapp.ui.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.colorspace.WhitePoint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.estudapp.R
import com.example.estudapp.ui.feature.auth.AuthState
import com.example.estudapp.ui.feature.auth.AuthViewModel
import com.example.estudapp.ui.theme.Black
import com.example.estudapp.ui.theme.ErrorRed
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
import com.example.estudapp.ui.theme.White

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
                .padding(top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row (
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(PrimaryBlue)
                    .padding(13.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bem vinda(o),\n[nome]", fontWeight = FontWeight.Black, fontSize = 22.sp, color = White)

                Image(painter = painterResource(id = R.drawable.logo_white), contentDescription = null, Modifier
                        .size(30.dp)
                        .align(Alignment.Bottom)
                )
            }

            Spacer(Modifier.height(2.dp))
            
            Text("Como você vai tirar notas melhores hoje?", color = PrimaryBlue, fontSize = 15.sp, modifier = Modifier.align(Alignment.Start).padding(start = 14.dp))

            Row (
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(LightGray)
                    .padding(15.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp)
                    .clickable(
                        onClick = { navController.navigate("deck_list") }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    Modifier.align(Alignment.Bottom)
                ) {
                    Text("Ver meus\nflashcards", color = PrimaryBlue, fontSize = 14.sp, lineHeight = 16.sp)
                    Text("Estudar", color = Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }

                Image(painter = painterResource(id = R.drawable.icon_flashcard), contentDescription = null, Modifier
                    .size(50.dp)
                )
            }

            Row (
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(LightGray)
                    .padding(15.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp)
                    .clickable(
                        onClick = { }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    Modifier.align(Alignment.Bottom)
                ) {
                    Text("Aprenda mais\ncom a MonitorIA", color = PrimaryBlue, fontSize = 14.sp, lineHeight = 16.sp)
                    Text("Tirar dúvidas", color = Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }

                Image(painter = painterResource(id = R.drawable.icon_robot), contentDescription = null, Modifier
                    .size(50.dp)
                )
            }

            Row (
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(LightGray)
                    .padding(15.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp)
                    .clickable(
                        onClick = { navController.navigate("profile") }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    Modifier.align(Alignment.Bottom)
                ) {
                    Text("Perfil e\nlocalizações", color = PrimaryBlue, fontSize = 14.sp, lineHeight = 16.sp)
                    Text("Seu perfil", color = Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }

                Image(painter = painterResource(id = R.drawable.icon_profile), contentDescription = null, Modifier
                    .size(50.dp)
                )
            }

            Spacer(Modifier.height(27.dp))

            Text("Estuda++", fontSize = 12.sp, color = PrimaryBlue)
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