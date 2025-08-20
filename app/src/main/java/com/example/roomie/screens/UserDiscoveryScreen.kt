package com.example.roomie.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.roomie.components.PreferenceWeights
import com.google.firebase.firestore.FieldValue

// String descriptions for each of the weights
private val weightLabels = arrayOf(
    "Indifferent",     // 0
    "Nice-to-have",    // 1
    "Slightly want",   // 2
    "Moderately want", // 3
    "Want a lot",      // 4
    "Must-have"        // 5
)

@Composable
fun UserDiscoveryScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var matches by remember { mutableStateOf<List<StudentProfile>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableStateOf(0) }
    var weights by remember { mutableStateOf(PreferenceWeights()) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val matchingService = MatchingService
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid

    // will reload every time weights change
    LaunchedEffect(weights) {
        try {
            errorMessage = null
            matches = matchingService.findMatchesForCurrentUser(weights)
            currentIndex = 0 // reset back to first profile when reranking
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

        Button(
            onClick = { showFilterDialog = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text("Adjust Match Preferences")
        }


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
                val currentProfile = matches!!.getOrNull(currentIndex)
                if (currentProfile != null) {
                    MatchCard(currentProfile)

                    Spacer(modifier = Modifier.height(12.dp))

                    Spacer(modifier = Modifier.weight(1f))

                    // Button: open/create 1:1 chat and navigate to chat page
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    if (currentUserId == null) {
                                        Log.e("UserDiscovery", "No current user id - cannot create chat")
                                        return@launch
                                    }

                                    val otherUserId = currentProfile.id

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
                                        val chatManager = ChatManager()
                                        chatManager.createConversation(
                                            listOf(currentUserId, otherUserId),
                                            isGroup = false
                                        )
                                        checkNotNull(chatManager.conversationId)
                                    }

                                    val chatTitle = Uri.encode(currentProfile.name)
                                    navController.navigate("chat/$conversationId/$chatTitle")
                                } catch (e: Exception) {
                                    Log.e("UserDiscovery", "Failed to open/create chat: ${e.message}", e)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text("Message this user")
                    }

                    // Button to go to next profile
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (currentUserId != null) {
                                    db.collection("users").document(currentUserId)
                                        .update("seenUsers", FieldValue.arrayUnion(currentProfile.id))
                                        .await()
                                }
                                if (currentIndex < matches!!.lastIndex) {
                                    currentIndex++
                                } else {
                                    errorMessage = "All users have been explored"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Next Profile")
                    }
                } else {
                    Text("All users have been explored")
                }
            }
        }

        if (showFilterDialog) {
            FilterDialog(
                current = weights,
                onDismiss = { showFilterDialog = false },
                onSave = {
                    weights = it
                    showFilterDialog = false
                }
            )
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
            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileChip(Icons.Default.School, profile.studentUniversity)
                ProfileChip(Icons.Default.Group, "Group Size: ${profile.studentDesiredGroupSize.joinToString(" - ")}")
                ProfileChip(Icons.Default.Commute, "${profile.studentMaxCommute} mins")
                ProfileChip(Icons.Default.AttachMoney, "$${profile.studentMaxBudget}")
            }

            Text(
                text = "Bio: ${profile.bio}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun ProfileChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        tonalElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(end = 6.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun FilterDialog(
    current: PreferenceWeights,
    onDismiss: () -> Unit,
    onSave: (PreferenceWeights) -> Unit
) {
    var localWeights by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onSave(localWeights) }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Set Match Importance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeightSlider("University", localWeights.university) {
                    localWeights = localWeights.copy(university = it)
                }
                WeightSlider("Budget", localWeights.budget) {
                    localWeights = localWeights.copy(budget = it)
                }
                WeightSlider("Commute", localWeights.commute) {
                    localWeights = localWeights.copy(commute = it)
                }
                WeightSlider("Group Size", localWeights.groupSize) {
                    localWeights = localWeights.copy(groupSize = it)
                }
                WeightSlider("Preferences", localWeights.preferences) {
                    localWeights = localWeights.copy(preferences = it)
                }
            }
        }
    )
}

@Composable
fun WeightSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Text("$label: ${weightLabels[value]}")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..5f,
            steps = 4 // gives 0..5 discrete
        )
    }
}

