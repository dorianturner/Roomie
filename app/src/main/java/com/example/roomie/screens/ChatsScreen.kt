package com.example.roomie.screens

import androidx.compose.foundation.background
import com.example.roomie.components.chat.ChatItem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

import androidx.compose.ui.Alignment

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn

import com.example.roomie.components.chat.ChatManager
import com.example.roomie.components.chat.ChatType
import com.example.roomie.components.chat.Conversation
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onBack: () -> Unit,
    navController: NavController
) {

    val conversations = remember { mutableStateListOf<Conversation>() }
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid ?: return

    LaunchedEffect(Unit) {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val allConvos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                }

                // Sort so MY_GROUP chats are first
                val sortedConvos = allConvos.sortedWith(
                    compareByDescending<Conversation> { it.chatType == ChatType.MY_GROUP }
                        .thenByDescending { it.lastMessageAt }
                )

                conversations.clear()
                conversations.addAll(sortedConvos)
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Messages") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(color = MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    },
                    isGroup = convo.isGroup,
                    chatType = convo.chatType
                )
            }
        }
    }
}
