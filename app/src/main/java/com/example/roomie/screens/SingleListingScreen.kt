package com.example.roomie.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.roomie.components.listings.Listing
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import com.example.roomie.components.listings.ListingDetailsContent
import com.example.roomie.components.listings.ListingPhotoGallery
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleListingScreen(
    listingId: String,
    onBack: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var listing by remember { mutableStateOf<Listing?>(null) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentListing = listing
                    if (currentListing != null && currentListing.ownerId == currentUserId) {
                        IconButton(
                            onClick = {
                                navController.navigate("edit_listing/${currentListing.id}")
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit listing")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        listing?.let { currentListing ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (currentListing.photos.isNotEmpty()) {
                    ListingPhotoGallery(
                        photos = currentListing.photos,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ListingDetailsContent(
                    listing = currentListing,
                    modifier = Modifier.weight(1f)
                )
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}