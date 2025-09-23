package com.example.roomie.screens

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roomie.components.GroupProfile
import com.example.roomie.components.MatchingService
import com.example.roomie.components.PreferenceWeights
import com.example.roomie.components.StudentProfile
import com.example.roomie.components.chat.ChatManager
import com.example.roomie.components.overlays.ProfileOnTap
import com.example.roomie.components.soundManager.LocalSoundManager
import com.example.roomie.components.userDiscovery.ProfileCard
import com.example.roomie.ui.theme.FontSize
import com.example.roomie.ui.theme.MontserratFontFamily
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

/**
 * Composable function for the User Discovery Screen.
 * This screen allows users to discover and match with other users or groups.
 * It features a swipeable card interface for liking or disliking profiles,
 * a filter dialog to set preference weights, and navigation to chat upon matching.
 *
 * @param navController NavController for navigating to other screens.
 * @param modifier Modifier for this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDiscoveryScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {

    var showOverlay by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf<List<GroupProfile>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var weights by remember { mutableStateOf(PreferenceWeights()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) } // trigger a reload of the matches

    val matchingService = MatchingService
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid
    val sounds = LocalSoundManager.current

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "User Discovery",
                fontFamily = MontserratFontFamily,
                color = MaterialTheme.colorScheme.inverseSurface,
                fontSize = FontSize.header
            )

            IconButton(onClick = { showFilterDialog = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            matches == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    strokeWidth = 5.dp,
                    color = MaterialTheme.colorScheme.surfaceTint
                )
            }
            errorMessage != null -> Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            matches!!.isEmpty() -> Text("No matches found")
            else -> {
                val currentProfile = matches!!.getOrNull(currentIndex)
                if (currentProfile != null) {
                    SwipeableMatchCard(
                        profile = currentProfile,
                        onSwiped = { dir ->
                            coroutineScope.launch {
                                if (currentUserId != null) {
                                    db.collection("users").document(currentUserId)
                                        .update(
                                            "seenUsersTimestamps.${currentProfile.id}",
                                            System.currentTimeMillis()
                                        )
                                        .await()
                                }

                                when (dir) {
                                    SwipeDirection.RIGHT -> {
                                        try {
                                            if (currentUserId == null) {
                                                Log.e(
                                                    "UserDiscovery",
                                                    "No current user id - cannot create chat"
                                                )
                                                return@launch
                                            }

                                            // play sound
                                            sounds.swipeRight()

                                            val db = FirebaseFirestore.getInstance()

                                            // Probably quite wasteful
                                            // Fetch current user to get groupId
                                            val currentUserSnapshot =
                                                db.collection("users").document(currentUserId).get()
                                                    .await()
                                            val currentUserGroupId =
                                                currentUserSnapshot.getString("groupId")
                                                    ?: run {
                                                        Log.e(
                                                            "UserDiscovery",
                                                            "Current user has no groupId"
                                                        )
                                                        return@launch
                                                    }

                                            // Fetch current user's group members
                                            val currentGroupSnapshot =
                                                db.collection("groups").document(currentUserGroupId)
                                                    .get().await()
                                            val membersRaw = currentGroupSnapshot.get("members") as? List<*>
                                            val currentGroupMembers = membersRaw
                                                ?.mapNotNull { raw ->
                                                    (raw as? Map<*, *>)?.mapNotNull { (k, v) ->
                                                        if (k is String) k to v else null
                                                    }?.toMap()
                                                } ?: emptyList()

                                            val currentGroupMemberIds =
                                                currentGroupMembers.mapNotNull { it["id"] as? String }

                                            // Get swiped group's members
                                            val otherGroupMembers =
                                                currentProfile.members.map { it.id }

                                            // Combine all member IDs
                                            val allParticipantIds =
                                                (currentGroupMemberIds + otherGroupMembers).distinct()
                                                    .toMutableList()
                                            if (!allParticipantIds.contains(currentUserId)) {
                                                allParticipantIds += currentUserId
                                            }

                                            // Check if a conversation already exists between these members
                                            val snapshot = db.collection("conversations")
                                                .whereArrayContains("participants", currentUserId)
                                                .get()
                                                .await()

                                            val existingDoc =
                                                snapshot.documents.firstOrNull { doc ->
                                                    val participants =
                                                        doc.get("participants") as? List<*>
                                                    participants?.toSet() == allParticipantIds.toSet()
                                                }

                                            // Create conversation if it doesn't exist
                                            val conversationId = if (existingDoc != null) {
                                                existingDoc.id
                                            } else {
                                                val chatManager = ChatManager()
                                                chatManager.createConversation(
                                                    participants = allParticipantIds,
                                                    isGroup = true
                                                )
                                                checkNotNull(chatManager.conversationId)
                                            }

                                            // Navigate to chat
                                            val chatTitle = Uri.encode(currentProfile.name)
                                            navController.navigate("chat/$conversationId/$chatTitle")

                                        } catch (
                                            e: Exception) {
                                            Log.e(
                                                "UserDiscovery",
                                                "Failed to open/create chat: ${e.message}",
                                                e
                                            )
                                        }
                                    }

                                    SwipeDirection.LEFT -> {

                                        // play sound
                                        sounds.swipeLeft()

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
                        },
                        onClick = {
                            showOverlay = true
                        }
                    )

                    // Profile screen pops up
                    if (showOverlay) {
                        val sheetState = rememberModalBottomSheetState(
                            skipPartiallyExpanded = true
                        )
                        ProfileOnTap(
                            sheetState,
                            { showOverlay = false },
                            groupProfile = currentProfile
                        )
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

/**
 * A composable function that displays a chip with an icon and text.
 * Used to display small pieces of information in a visually appealing way.
 *
 * @param icon The icon to display in the chip.
 * @param text The text to display in the chip.
 */
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
                contentDescription = null, // Icon is decorative
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

/**
 * A composable function that displays a match card which can be swiped left or right.
 * Swiping right indicates a "like" and may initiate a chat.
 * Swiping left indicates a "dislike" and moves to the next profile.
 * Clicking the card shows more details.
 *
 * @param profile The [GroupProfile] to display on the card.
 * @param onSwiped Callback function triggered when the card is swiped, providing the [SwipeDirection].
 * @param onClick Callback function triggered when the card is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableMatchCard(
    profile: GroupProfile,
    onSwiped: (SwipeDirection) -> Unit,
    onClick: () -> Unit
) {
    val density = LocalDensity.current

    val screenWidthPx = with(density) { LocalWindowInfo.current.containerSize.width.dp.toPx() }
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
            state.animateTo(SwipeDirection.NONE, tween(300, 20))
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
            )
            .combinedClickable(
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
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
            ProfileCard(profile)
        }
    }
}

/**
 * A composable function that displays detailed information about a group,
 * including its name, size, average stats, and a list of its members.
 *
 * @param group The [GroupProfile] to display.
 */
@Composable
fun GroupMatchCard(group: GroupProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title - make it clear this is a group
            Text(
                text = if (group.stats.size == 1) "User: ${group.name}" else "Group: ${group.name} (${group.stats.size} members)",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Group-level stats
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileChip(Icons.Default.Group, "Size: ${group.stats.size}")
                ProfileChip(Icons.Default.Commute, "Avg commute: ${group.stats.avgCommute} mins")
                ProfileChip(Icons.Default.AttachMoney, "Avg budget: $${group.stats.avgBudget}")
                ProfileChip(Icons.Default.Cake, "Avg age: ${group.stats.avgAge}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontally scrollable member list
            Text(
                text = "Members",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                group.members.forEach { member ->
                    Log.d("GroupMatchCard", "Member: ${member.name}, Age: ${member.studentAge}")
                    MemberMiniCard(member)
                }
            }
        }
    }
}

/**
 * A composable function that displays a compact card for a single student member,
 * showing their name, bio (if available), age, university, commute time, and budget.
 *
 * @param profile The [StudentProfile] of the member to display.
 */
@Composable
fun MemberMiniCard(profile: StudentProfile) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium)
            Text(profile.bio ?: "", style = MaterialTheme.typography.bodyMedium)
            ProfileChip(Icons.Default.Cake, profile.studentAge.toString())
            ProfileChip(Icons.Default.School, profile.studentUniversity ?: "")
            ProfileChip(Icons.Default.Commute, "${profile.studentMaxCommute} mins")
            ProfileChip(Icons.Default.AttachMoney, "$${profile.studentMaxBudget}")

        }
    }
}


/**
 * A composable function that displays a dialog for users to set their preference weights
 * for various matching criteria.
 *
 * @param current The current [PreferenceWeights] to initialize the dialog with.
 * @param onDismiss Callback function triggered when the dialog is dismissed without saving.
 * @param onSave Callback function triggered when the "Save" button is clicked, providing the updated [PreferenceWeights].
 */
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
                WeightSlider("Age", localWeights.age) {
                    localWeights = localWeights.copy(age = it)
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
                WeightSlider("Alcohol", localWeights.alcohol) {
                    localWeights = localWeights.copy(alcohol = it)
                }
                WeightSlider("Has Profile Picture", localWeights.profilePicture) {
                    localWeights = localWeights.copy(profilePicture = it)
                }
            }
        }
    )
}

/**
 * A composable function that displays a slider for adjusting a specific preference weight.
 * It shows a label for the criteria and the current descriptive value of the weight (e.g., "Must-have").
 *
 * @param label The label for the preference criteria (e.g., "Age", "Budget").
 * @param value The current integer value of the weight (0-5).
 * @param onValueChange Callback function triggered when the slider value changes, providing the new integer value.
 */
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
