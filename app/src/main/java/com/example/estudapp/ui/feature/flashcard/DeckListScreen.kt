package com.example.estudapp.ui.feature.flashcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.estudapp.R
import com.example.estudapp.data.model.DeckDTO
import com.example.estudapp.ui.theme.Black
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
import com.example.estudapp.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    navController: NavHostController,
    deckViewModel: DeckViewModel = viewModel()
) {
    val uiState by deckViewModel.decksState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ){
                        Icon(Icons.Outlined.KeyboardArrowLeft, "goBack", tint = PrimaryBlue, modifier = Modifier.size(35.dp))
                    }
                },
                title = { Text("Estudar", color = PrimaryBlue, fontWeight = FontWeight.Black) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 20.dp, bottom = 20.dp, start = 30.dp, end = 30.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is DecksUiState.Loading -> {
                    Spacer(Modifier.fillMaxHeight(0.5f))
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                is DecksUiState.Error -> {
                    Spacer(Modifier.fillMaxHeight(0.5f))
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30f))
                            .background(LightGray)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        Icon(Icons.Outlined.Warning, contentDescription = "warning", tint = PrimaryBlue)
                        Text(state.message, color = PrimaryBlue, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }
                }
                is DecksUiState.Success -> {
                    Row (
                        modifier = Modifier
                            .clip(RoundedCornerShape(30f))
                            .background(PrimaryBlue)
                            .padding(13.dp)
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable(
                                onClick = { navController.navigate("create_deck") }
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = White, modifier = Modifier.size(40.dp))

                        Spacer(Modifier.width(15.dp))

                        Text(text = "Criar novo deck", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = White)
                    }

                    if (state.decks.isEmpty()) {
                        Spacer(Modifier.fillMaxHeight(0.4f))
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30f))
                                .background(LightGray)
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ){
                            Icon(Icons.Outlined.Warning, contentDescription = "warning", tint = PrimaryBlue)
                            Text("você ainda não\ntem nenhum deck :\\", color = PrimaryBlue, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        Spacer(Modifier.fillMaxHeight(0.1f))

                        Text("Meus decks", color = PrimaryBlue, fontSize = 17.sp, modifier = Modifier.align(Alignment.Start))
                        Spacer(Modifier.height(5.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            //contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.decks) { deck ->
                                DeckItem(
                                    navController = navController,
                                    deck = deck
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeckItem(
    navController: NavHostController,
    deck: DeckDTO
) {
    Row (
        modifier = Modifier
            .clip(RoundedCornerShape(30f))
            .background(LightGray)
            .padding(13.dp)
            .fillMaxWidth()
            .height(40.dp)
            .clickable(
                onClick = { navController.navigate("flashcard_list/${deck.id}") }
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(id = R.drawable.icon_decks), contentDescription = null, Modifier
            .size(40.dp)
        )
        Spacer(Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = deck.name, style = MaterialTheme.typography.titleMedium, color = Black)
            deck.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = Black)
            }
        }

        Button(
            onClick = { navController.navigate("study_session/${deck.id}") },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(30f),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Começar", color = White, fontSize = 13.sp)
        }
    }
}