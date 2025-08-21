package com.example.roomie.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color

import com.example.roomie.components.ChatManager
import com.example.roomie.components.Conversation
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onBack: () -> Unit, // callback to return to main content screen
    navController: NavController
) {

    // To be updated to reflect the firestore database
    val conversations = remember { mutableStateListOf<Conversation>() }
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid ?: return

    LaunchedEffect(Unit) {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                conversations.clear()
                conversations.addAll(snapshot.documents.mapNotNull {
                    val convo = it.toObject(Conversation::class.java)
                    convo?.copy(id = it.id)
                })
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
            )
        }
    ) { innerPadding ->
        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Temporarily red so i can see whats going on
                .background(color = MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chat content to be added here
            items(conversations) { convo ->
                val chatManager = remember { ChatManager(convo.id) }

                val title by produceState(initialValue = "Loading...", convo.participants) {
                    value = chatManager.getConversationTitle(currentUserId)
                }

                ChatItem(
                    name = title,
                    lastMessage = convo.lastMessage.orEmpty(),
                    time = convo.lastMessageAt,
                    onClick = {
                        navController.navigate("chat/${convo.id}/$title")
                    }
                )
            }
        }
    }
}