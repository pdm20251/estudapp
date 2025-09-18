package com.example.estudapp.ui.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.estudapp.R
import com.example.estudapp.ui.feature.auth.AuthState
import com.example.estudapp.ui.feature.auth.AuthViewModel
import com.example.estudapp.ui.feature.location.LocationViewModel
import com.example.estudapp.ui.theme.Black
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
import com.example.estudapp.ui.theme.White

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    locationViewModel: LocationViewModel // Vírgula adicionada aqui
) {
    val authState = authViewModel.authState.observeAsState()
    val currentLocation by locationViewModel.currentGeofenceLocation.collectAsState()

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login") {
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
                .padding(vertical = 20.dp)
                .verticalScroll(rememberScrollState()), // Adicionado para rolagem
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(PrimaryBlue)
                    .padding(13.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bem vinda(o),\n[nome]", fontWeight = FontWeight.Black, fontSize = 22.sp, color = White)
                Image(painter = painterResource(id = R.drawable.logo_white), contentDescription = null, modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Bottom))
            }

            Spacer(Modifier.height(20.dp))

            // Card de Localização Dinâmico
            currentLocation?.let { locationName ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30f))
                        .background(LightGray)
                        .padding(15.dp)
                        .fillMaxWidth(0.92f)
                        .height(100.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.align(Alignment.Bottom)) {
                        Text("Local de estudo atual", color = PrimaryBlue, fontSize = 14.sp, lineHeight = 16.sp)
                        Text(locationName, color = Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Image(painter = painterResource(id = R.drawable.icon_map_pin), contentDescription = "Ícone de Localização", modifier = Modifier.size(50.dp))
                }
                Spacer(Modifier.height(10.dp))
            }

            Text("Como você vai tirar notas melhores hoje?", color = PrimaryBlue, fontSize = 15.sp, modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 25.dp))
            Spacer(Modifier.height(10.dp))

            // Card "Estudar"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(LightGray)
                    .padding(15.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp)
                    .clickable { navController.navigate("deck_list") },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.align(Alignment.Bottom)) {
                    Text("Ver os\nmeus decks", color = PrimaryBlue, fontSize = 14.sp, lineHeight = 16.sp)
                    Text("Estudar", color = Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Image(painter = painterResource(id = R.drawable.icon_flashcard), contentDescription = null, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(10.dp))

            // Card "MonitorIA"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(LightGray)
                    .padding(15.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp)
                    .clickable(
                        onClick = { navController.navigate("chat") }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.align(Alignment.Bottom)) {
                    Text("Aprenda mais\ncom a MonitorIA", color = PrimaryBlue, fontSize = 14.sp, lineHeight = 16.sp)
                    Text("Tirar dúvidas", color = Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Image(painter = painterResource(id = R.drawable.icon_robot), contentDescription = null, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(10.dp))

            // Card "Perfil"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30f))
                    .background(LightGray)
                    .padding(15.dp)
                    .fillMaxWidth(0.92f)
                    .height(100.dp)
                    .clickable { navController.navigate("profile") },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.align(Alignment.Bottom)) {
                    Text("Perfil e\nlocalizações", color = PrimaryBlue, fontSize = 14.sp, lineHeight = 16.sp)
                    Text("Seu perfil", color = Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Image(painter = painterResource(id = R.drawable.icon_profile), contentDescription = null, modifier = Modifier.size(50.dp))
            }

            Spacer(Modifier.height(27.dp))
            Text("Estuda++", fontSize = 12.sp, color = PrimaryBlue)
        }
    }
}