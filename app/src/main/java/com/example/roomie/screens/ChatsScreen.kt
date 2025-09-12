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
import com.example.roomie.components.chat.Conversation
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

    val conversations = remember { mutableStateListOf<Conversation>() }
    val groupConversation = remember { mutableStateOf<Conversation?>(null) }
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid ?: return

    LaunchedEffect(Unit) {
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val allConversations = snapshot.documents.mapNotNull {
                    val convo = it.toObject(Conversation::class.java)
                    convo?.copy(id = it.id)
                }.sortedByDescending { it.participants.size }

                val (groups, individualConversations) = allConversations.partition { it.isGroup }


                // user should only be in one group
                for (convo in conversations) {
                    println("ID: ${convo.id}\n isGroup: ${convo.isGroup}\n")
                }

                conversations.clear()
                conversations.addAll(individualConversations)
                groupConversation.value = groups.firstOrNull()
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
                .background(color = MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // group conversation is displayed specially
            groupConversation.value?.let { group ->
                item {
                    val chatManager = remember { ChatManager(group.id) }

                    val participants by produceState(initialValue = "Loading...", group.participants) {
                        value = chatManager.getConversationTitle(currentUserId)
                    }

                    ChatItem(
                        name = "MY GROUP",
                        lastMessage = group.lastMessage.orEmpty(),
                        time = group.lastMessageAt,
                        onClick = {
                            navController.navigate("chat/${group.id}/$participants")
                        },
                        isGroup = true,
                        groupParticipants = participants,
                    )
                }
            }

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