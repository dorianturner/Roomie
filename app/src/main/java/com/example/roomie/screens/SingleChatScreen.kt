package com.example.roomie.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.roomie.components.ChatManager
import com.example.roomie.components.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleChatScreen(
    chatRepository: ChatManager,
    chatName: String, // passed to save recomputing
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messagesState = remember { mutableStateListOf<Message>() }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val userID: String? = remember { Firebase.auth.currentUser?.uid }

    // Listen for messages
    DisposableEffect(chatRepository) {
        val registration = chatRepository.listenMessages { messages ->
            messagesState.clear()
            messagesState.addAll(messages)
        }
        onDispose {
            registration.remove()
        }
    }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                pickedImageUri = it
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = chatName,
                    modifier = Modifier.padding(4.dp)
                ) },
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
        Column(modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(8.dp)
            ) {
                items(messagesState.reversed()) { msg ->
                    Text(
                        text = msg.text ?: "[${msg.type}]",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            // Input + send button + image picker
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {

                // Image picker (launches system picker)
                IconButton(onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) {
                    Icon(Icons.Filled.Attachment, contentDescription = "Pick Image")
                }

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") }
                )

                IconButton(onClick = {
                    coroutineScope.launch {
                        if (pickedImageUri != null) {
                            chatRepository.sendMessage(
                                userID!!,
                                mediaUri = pickedImageUri,
                                type = "image"
                            )
                            pickedImageUri = null
                        } else if (inputText.isNotBlank()) {
                            chatRepository.sendMessage(userID!!, inputText)
                        }
                        inputText = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
