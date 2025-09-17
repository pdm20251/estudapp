// Crie este novo arquivo em ui/feature/flashcard/CreateDeckScreen.kt
package com.example.estudapp.ui.feature.flashcard

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.estudapp.R
import com.example.estudapp.ui.theme.ErrorRed
import com.example.estudapp.ui.theme.LightGray
import com.example.estudapp.ui.theme.PrimaryBlue
import com.example.estudapp.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeckScreen(
    navController: NavHostController,
    deckViewModel: DeckViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current

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
                title = { Text("Novo deck", color = PrimaryBlue, fontWeight = FontWeight.Black) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(id = R.drawable.icon_decks), contentDescription = null, Modifier
                .size(80.dp)
            )
            Spacer(Modifier.height(100.dp))

            Text(
                text = "Nome do deck", color = PrimaryBlue,
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
                placeholder = { Text("Ex.: Biologia") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = PrimaryBlue,
                    cursorColor = PrimaryBlue,
                    errorIndicatorColor = ErrorRed,
                    unfocusedPlaceholderColor = PrimaryBlue,
                    focusedPlaceholderColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(30f)
            )
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Descrição", color = PrimaryBlue,
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
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("(Opcional)") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = PrimaryBlue,
                    cursorColor = PrimaryBlue,
                    errorIndicatorColor = ErrorRed,
                    unfocusedPlaceholderColor = PrimaryBlue,
                    focusedPlaceholderColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(30f)
            )
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(55.dp),
                onClick = {
                    deckViewModel.createDeck(name, description)
                    Toast.makeText(context, "Deck '$name' criado!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(30f),
                enabled = (name.length >= 3)
            ) {
                Text(text = "Criar", fontSize = 18.sp, color = White)
            }
        }
    }
}