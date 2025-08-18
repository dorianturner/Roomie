package com.example.roomie.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roomie.components.StudentProfile
import com.example.roomie.components.MatchingService
import com.example.roomie.components.ChatManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun UserDiscoveryScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var matches by remember { mutableStateOf<List<StudentProfile>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val matchingService = MatchingService
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid

    LaunchedEffect(Unit) {
        try {
            matches = matchingService.findMatchesForCurrentUser()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            Log.e("UserDiscoveryScreen", "Error fetching matches", e)
            matches = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("User Discovery", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        when {
            matches == null -> {
                CircularProgressIndicator()
            }
            errorMessage != null -> {
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
            matches!!.isEmpty() -> {
                Text("No matches found")
            }
            else -> {
                // SHOW ONLY THE FIRST MATCH (if any)
                val first = matches!!.firstOrNull()
                if (first != null) {
                    MatchCard(first)

                    Spacer(modifier = Modifier.height(12.dp))

                    // push button to bottom
                    Spacer(modifier = Modifier.weight(1f))

                    // Button: open/create 1:1 chat and navigate to chat page
                    Button(
                        onClick = {
                            // create or find convo then navigate
                            coroutineScope.launch {
                                try {
                                    if (currentUserId == null) {
                                        Log.e("UserDiscovery", "No current user id - cannot create chat")
                                        return@launch
                                    }

                                    // NOTE: change `studentId` if your StudentProfile uses a different field name
                                    val otherUserId = first.id

                                    // Try to find existing conversation involving both users (client-side filter)
                                    val snapshot = db.collection("conversations")
                                        .whereArrayContains("participants", currentUserId)
                                        .get()
                                        .await()

                                    val existingDoc = snapshot.documents.firstOrNull { doc ->
                                        val participants = doc.get("participants") as? List<*>
                                        participants?.contains(otherUserId) == true && (participants.size == 2)
                                    }

                                    val conversationId = if (existingDoc != null) {
                                        existingDoc.id
                                    } else {
                                        // create new conversation
                                        val chatManager = ChatManager()
                                        chatManager.createConversation(listOf(currentUserId, otherUserId), isGroup = false)
                                        checkNotNull(chatManager.conversationId)
                                    }

                                    // Navigate to chat screen; encode the display name safely
                                    val chatTitle = Uri.encode(first.studentUniversity)
                                    navController.navigate("chat/$conversationId/$chatTitle")
                                } catch (e: Exception) {
                                    Log.e("UserDiscovery", "Failed to open/create chat: ${e.message}", e)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text("Message this user")
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(profile: StudentProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("University: ${profile.studentUniversity}", style = MaterialTheme.typography.bodyLarge)
            Text("Preferences: ${profile.studentBasicPreferences.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            Text("Desired Group Size: ${profile.studentDesiredGroupSize.joinToString(" - ")}", style = MaterialTheme.typography.bodyMedium)
            Text("Max Commute: ${profile.studentMaxCommute} mins", style = MaterialTheme.typography.bodyMedium)
            Text("Max Budget: $${profile.studentMaxBudget}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
