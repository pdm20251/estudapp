package com.example.estudapp.ui.feature.flashcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
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
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.data.model.FlashcardTypeEnum
import com.example.estudapp.ui.theme.Black
import com.example.estudapp.ui.theme.DarkBlue
import com.example.estudapp.ui.theme.ErrorRed
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
import com.example.estudapp.ui.theme.White
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardListScreen(
    navController: NavHostController,
    flashcardViewModel: FlashcardViewModel = viewModel(),
    deckId: String,
    deckName: String,
    deckDesc: String?
) {
    LaunchedEffect(deckId) {
        flashcardViewModel.loadFlashcards(deckId)
    }

    val uiState by flashcardViewModel.flashcardsState.collectAsState()

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
                title = { Text("Meus decks", color = PrimaryBlue, fontWeight = FontWeight.Black) },
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
                is FlashcardsUiState.Loading -> {
                    Spacer(Modifier.fillMaxHeight(0.5f))
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                is FlashcardsUiState.Error -> {
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
                is FlashcardsUiState.Success -> {
                    Text(deckName ?: "Error", fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))
                    Text(deckDesc ?: "", fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(30.dp))

                    Row (
                        modifier = Modifier
                            .clip(RoundedCornerShape(30f))
                            .background(DarkBlue)
                            .padding(13.dp)
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable(
                                onClick = { navController.navigate("study_session/${deckId}") },
                                enabled = !state.flashcards.isEmpty()
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painter = painterResource(id = R.drawable.icon_thunderbolt), contentDescription = null, Modifier
                            .size(40.dp))

                        Spacer(Modifier.width(15.dp))

                        Text(text = "Começar a estudar", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = White)
                    }
                    Spacer(Modifier.height(18.dp))

                    Row (
                        modifier = Modifier
                            .clip(RoundedCornerShape(30f))
                            .background(PrimaryBlue)
                            .padding(13.dp)
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable(
                                onClick = { navController.navigate("create_flashcard/$deckId") }
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = White, modifier = Modifier.size(40.dp))

                        Spacer(Modifier.width(15.dp))

                        Text(text = "Criar novo card", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = White)
                    }
                    if (state.flashcards.isEmpty()) {
                        Spacer(Modifier.fillMaxHeight(0.4f))
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30f))
                                .background(LightGray)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ){
                            Icon(Icons.Outlined.Info, contentDescription = "info", tint = PrimaryBlue)
                            Spacer(Modifier.height(4.dp))
                            Text("Esse deck ainda não\ntem nenhum flashcard :\\", color = PrimaryBlue, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        Spacer(Modifier.fillMaxHeight(0.15f))

                        Text("Flashcards neste deck", color = PrimaryBlue, modifier = Modifier.align(Alignment.Start))
                        Spacer(Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            //contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.flashcards) { flashcard ->
                                FlashcardItem(
                                    flashcard = flashcard,
                                    onDeleteClick = {
                                        flashcardViewModel.deleteFlashcard(deckId, flashcard.id)
                                    },
                                    onEditClick = {
                                        navController.navigate("create_flashcard/${deckId}?flashcardId=${flashcard.id}")
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    TextButton(
                        onClick = {
                            flashcardViewModel.deleteDeck(deckId)
                            navController.popBackStack()
                        },
                    ) {
                        Text("Apagar deck", color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.Bold )
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardItem(
    flashcard: FlashcardDTO,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row (
        modifier = Modifier
            .border(
                width = 1.dp,
                color = LightGray,
                shape = RoundedCornerShape(30f)
            )
            .padding(8.dp)
            .fillMaxWidth()
            //.height(60.dp)
            .clickable(
                onClick = onEditClick
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                text = flashcard.type.replace("_", " / ").lowercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(1.dp))
            val title = when (flashcard.type) {
                FlashcardTypeEnum.FRENTE_VERSO.name -> flashcard.frente
                FlashcardTypeEnum.CLOZE.name -> flashcard.textoComLacunas?.replace(Regex("\\{\\{(c\\d+)::.*?\\}\\}"), "{{....}}")
                else -> flashcard.pergunta
            }
            Text(text = title ?: "Flashcard inválido", fontSize = 14.sp)
        }

        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Deletar Flashcard", tint = ErrorRed)
        }
    }
}