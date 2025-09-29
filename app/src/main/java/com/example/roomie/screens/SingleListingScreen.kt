package com.example.roomie.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roomie.components.chat.ChatManager
import com.example.roomie.components.listings.Listing
import com.example.roomie.components.listings.ListingDetailsContent
import com.example.roomie.components.listings.ListingPhotoGallery
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleListingScreen(
    listingId: String,
    onBack: () -> Unit,
    navController: NavController
) {
    var listing by remember { mutableStateOf<Listing?>(null) }
    var isLoadingChat by remember { mutableStateOf(false) }
    var existingConversationId by remember { mutableStateOf<String?>(null) }
    var checkingExistingChat by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(listingId) {
        db.collection("listings")
            .document(listingId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                listing = snapshot.toObject(Listing::class.java)?.copy(id = snapshot.id)
            }
    }

    // check for existing conversation when listing loads
    val currentListing = listing
    LaunchedEffect(currentListing?.ownerId, currentUserId) {
        if (currentListing != null && currentUserId != null &&
            currentUserId != currentListing.ownerId &&
            currentListing.ownerId.isNotEmpty()
        ) {
            checkingExistingChat = true
            try {
                val existingConvo =
                    findExistingConversation(currentUserId, currentListing.ownerId, db)
                existingConversationId = existingConvo?.id
            } catch (e: Exception) {
                // error
            } finally {
                checkingExistingChat = false
            }
        }
    }

    // chat creation with landlord
    LaunchedEffect(isLoadingChat) {
        if (isLoadingChat && currentListing != null && currentUserId != null) {
            try {

                // if we already have an existing conversation ID, navigate immediately
                if (findExistingConversation(currentUserId, currentListing.ownerId, db)?.id != null) {
                    val chatManager = ChatManager(existingConversationId)
                    val chatTitle = chatManager.getConversationTitle(currentUserId)
                    navController.navigate("chat/${existingConversationId}/$chatTitle") {
                        // clear the backstack
                        popUpTo(
                            navController.currentBackStackEntry?.destination?.route ?: "listings"
                        ) {
                            inclusive = false
                        }
                    }
                    isLoadingChat = false
                    return@LaunchedEffect
                }

                // Otherwise, create a new conversation
                val chatManager = ChatManager()
                val participants = listOf(currentUserId, currentListing.ownerId)

                chatManager.createConversation(
                    participants = participants,
                    isGroup = false,
                )

                // get conversation title and navigate to chat
                val chatTitle = chatManager.getConversationTitle(currentUserId)
                navController.navigate("chat/${chatManager.conversationId}/$chatTitle") {
                    // clear backstack
                    popUpTo(navController.currentBackStackEntry?.destination?.route ?: "listings") {
                        inclusive = false
                    }
                }

                // Update the existing conversation ID for future reference
                existingConversationId = chatManager.conversationId
            } catch (e: Exception) {
                // Handle error (you might want to show a snackbar or toast)
            } finally {
                isLoadingChat = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentListing = listing
                    if (currentListing != null && currentListing.ownerId == currentUserId) {
                        IconButton(
                            onClick = { navController.navigate("edit_listing/${currentListing.id}") }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit listing")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // single vertical scroller for the whole page
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding) // ensures content sits below TopAppBar
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // loading state
            if (listing == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                val currentListing = listing!!

                // photo gallery header (fixed height)
                if (currentListing.photos.isNotEmpty()) {
                    item {
                        ListingPhotoGallery(
                            photos = currentListing.photos,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                        )
                    }
                }

                // details as a normal item (no weight)
                item {
                    ListingDetailsContent(
                        listing = currentListing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // Message button section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                isLoadingChat = true
                                navController.navigate("single_listing/${currentListing.id}")
                            },
                            enabled = !isLoadingChat && !checkingExistingChat
                        ) {
                            if (isLoadingChat || checkingExistingChat) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .padding(end = 8.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Message,
                                    contentDescription = "Message",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(
                                when {
                                    checkingExistingChat -> "Checking..."
                                    isLoadingChat -> "Opening chat..."
                                    existingConversationId != null -> "Open Chat"
                                    else -> "Message Landlord"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// check if there is already a chat between user and landlord
private suspend fun findExistingConversation(
    currentUserId: String,
    landlordId: String,
    db: FirebaseFirestore
): com.example.roomie.components.chat.Conversation? {
    try {
        // filter conversations that have both participants
        val snapshot = db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .get()
            .await()

        val conversations = snapshot.documents.mapNotNull { doc ->
            doc.toObject(com.example.roomie.components.chat.Conversation::class.java)
                ?.copy(id = doc.id)
        }

        // filter for 1-on-1 conversations with the specific landlord
        val existingConvo = conversations.firstOrNull { conversation ->
            val isOneOnOne = conversation.participants.size == 2 && !conversation.isGroup
            val hasLandlord = conversation.participants.contains(landlordId)
            val hasCurrentUser = conversation.participants.contains(currentUserId)

            isOneOnOne && hasLandlord && hasCurrentUser
        }

        return existingConvo

    } catch (e: Exception) {
        return null
    }
}