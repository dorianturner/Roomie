package com.example.roomie.screens

import com.example.roomie.components.ChatItem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit, // callback to return to main content screen
    navController: NavController? = null  // Optional for navigation
) {

    // To be updated to reflect the firestore database
    val chats = remember {
        listOf(
            Chat("001", "Leo Matteucci", "Hello", "10:30 AM"),
            Chat("002", "Lucas Hutton", "Hi", "Yesterday"),
            Chat("003", "Dorian Turner", "I think we should pick this flat", "Monday")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    // needs to be updated with correct route
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chat content to be added here
            items(chats) { chat ->
                ChatItem(
                    name = chat.name,
                    lastMessage = chat.lastMessage,
                    time = chat.time,
                    onClick = {
                        // Navigate to specific chat
                        // navController?.navigate("chat_detail/${chat.id}")
                    }
                )
            }
        }
    }
}

data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String
)