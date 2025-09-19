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
import com.example.roomie.components.listings.Listing
import com.example.roomie.components.listings.ListingDetailsContent
import com.example.roomie.components.listings.ListingPhotoGallery
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleListingScreen(
    listingId: String,
    onBack: () -> Unit,
    navController: NavController
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
            }
        }
    }
}
