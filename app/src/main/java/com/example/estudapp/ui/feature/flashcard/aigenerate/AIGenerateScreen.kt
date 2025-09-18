package com.example.estudapp.ui.feature.flashcard.aigenerate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.estudapp.R
import com.example.estudapp.ui.feature.flashcard.FlashcardViewModel
import com.example.estudapp.ui.theme.Black
import com.example.estudapp.ui.theme.ErrorRed
import com.example.estudapp.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGenerateScreen(
    navController: NavHostController,
    flashcardViewModel: FlashcardViewModel = viewModel(),
    deckId: String,
    flashcardId: String?
) {
    var cardTypeIndex by remember { mutableIntStateOf(0) }
    val cardOptions = listOf("Frente / Verso", "Cloze", "Digite a resposta", "Múltipla escolha")
    var prompt by remember {
        mutableStateOf("")
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
                title = {
                    Text("AI Generate", color = PrimaryBlue, fontWeight = FontWeight.Black)
                },
                actions = {
                    Icon(painter = painterResource(R.drawable.icon_generate), contentDescription = "AI", tint = PrimaryBlue)
                    Spacer(Modifier.width(20.dp))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Selecione o tipo de Flashcard:", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start), color = PrimaryBlue)
                (0..3).forEach { index ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cardTypeIndex == index,
                            onClick = { cardTypeIndex = index },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryBlue
                            )
                        )
                        Text(cardOptions[index])
                    }
                }
            }

            Text(
                text = "Prompt", color = PrimaryBlue,
                fontSize = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .align(Alignment.Start)
            )

            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(55.dp),
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text("Ex.: geografia geral") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = PrimaryBlue,
                    cursorColor = PrimaryBlue,
                    errorIndicatorColor = ErrorRed
                ),
                shape = RoundedCornerShape(30f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
        }
    }
}