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

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs

// String descriptions for each of the weights
private val weightLabels = arrayOf(
    "Indifferent",     // 0
    "Nice-to-have",    // 1
    "Slightly want",   // 2
    "Moderately want", // 3
    "Want a lot",      // 4
    "Must-have"        // 5
)

enum class SwipeDirection { LEFT, RIGHT, NONE }

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
    var reloadKey by remember { mutableStateOf(0) } // trigger a reload of the matches

    val matchingService = MatchingService
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid

    // will reload every time weights change
    LaunchedEffect(weights, reloadKey) {
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

        Spacer(modifier = Modifier.height(16.dp))

        when {
            matches == null -> CircularProgressIndicator()
            errorMessage != null -> Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            matches!!.isEmpty() -> Text("No matches found")
            else -> {
                val currentProfile = matches!!.getOrNull(currentIndex)
                if (currentProfile != null) {
                    SwipeableMatchCard(
                        profile = currentProfile,
                        onSwiped = {
                            dir ->
                            coroutineScope.launch {
                                if (currentUserId != null) {
                                    db.collection("users").document(currentUserId)
                                        .update("seenUsersTimestamps.${currentProfile.id}", System.currentTimeMillis())
                                        .await()
                                }

                                when (dir) {
                                    SwipeDirection.RIGHT -> {
                                        // YES - create or open chat
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
                                    SwipeDirection.LEFT -> {
                                        if (currentIndex < matches!!.lastIndex) {
                                            currentIndex++
                                        } else {
                                            // bump reloadKey to retrigger LaunchedEffect to reload matches
                                            reloadKey++
                                        }
                                    }
                                    SwipeDirection.NONE -> {} // No action
                                }
                            }
                        }
                    )
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
fun SwipeableMatchCard(
    profile: StudentProfile,
    onSwiped: (SwipeDirection) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val swipeDistancePx = screenWidthPx / 2f

    // Anchors for swipe directions
    val anchors = remember(swipeDistancePx) {
        DraggableAnchors {
            SwipeDirection.LEFT at -swipeDistancePx
            SwipeDirection.NONE at 0f
            SwipeDirection.RIGHT at swipeDistancePx
        }
    }

    val state = remember(anchors) {
        AnchoredDraggableState(
            initialValue = SwipeDirection.NONE,
            anchors = anchors
        )
    }

    // Trigger callback when fully swiped
    LaunchedEffect(state.settledValue) {
        if (state.currentValue == SwipeDirection.LEFT || state.currentValue == SwipeDirection.RIGHT) {
            onSwiped(state.currentValue)
            state.animateTo(SwipeDirection.NONE, tween(300, 20)) // doesnt work
            // state.snapTo(SwipeDirection.NONE)
        }
    }

    val offsetX = state.requireOffset()
    val progress = (offsetX / swipeDistancePx).coerceIn(-1f, 1f)

    val rotation = progress * 10f // max ±10 degrees
    val alphaValue = 1f - abs(progress) * 0.5f // slightly fade out
    val bgColor = when {
        progress > 0f -> Color.Green.copy(alpha = abs(progress) * 0.3f)
        progress < 0f -> Color.Red.copy(alpha = abs(progress) * 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp) // adjust card height
            .background(bgColor)
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX
                    rotationZ = rotation
                    alpha = alphaValue
                    transformOrigin = TransformOrigin(0.5f, 1f) // bottom center
                }
        ) {
            MatchCard(profile)
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