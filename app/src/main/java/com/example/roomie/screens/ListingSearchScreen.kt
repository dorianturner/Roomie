package com.example.roomie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.roomie.components.listings.Listing
import com.example.roomie.components.listings.ListingItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.navigation.NavController
import com.example.roomie.components.listings.Group
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySearchScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
) {

    val listings = remember { mutableStateListOf<Listing>() }
    val group = remember { mutableStateOf<Group?>(null) }
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid ?: return
    var groupId: String? = null

    LaunchedEffect(Unit) {
        try {
            val document = db.collection("users").document(currentUserId).get().await()
            if (document.exists()) {
                groupId = document.getString("groupId")
            }
        } catch (e: Exception) {
            // exception (shouldn't happen if data formatted correctly)
        }

        db.collection("listings")
            .orderBy("rent", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                listings.clear()
                listings.addAll(snapshot.documents.mapNotNull {
                    val listing = it.toObject(Listing::class.java)
                    listing?.copy(id = it.id)
                })
            }
        db.collection("group")
            .whereEqualTo("GroupId", groupId)
            .limit(1) // Only fetch one document
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                if (!snapshot.isEmpty) {
                    val document = snapshot.documents[0]
                    val stats = document.get("stats") as? Map<String, Any>

                    group.value = Group(
                        id = document.id,
                        size = stats?.get("size") as Long,
                        minBudget = stats["minBudget"] as Long,
                        maxBudget = stats["maxBudget"] as Long,
                        minCommute = stats["minCommute"] as Long,
                        maxCommute = stats["maxCommute"] as Long,
                    )
                } else {
                    group.value = null // user not in a group
                }
            }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listings") },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(color = MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var filtered = false
            items(listings) { listing ->
                if (group.value != null) {
                    // compiler gets upset without the !!'s
                    // maybe because it's technically a value that can change concurrently even though it won't)
                    // using 1.3 as a buffer to show slightly more
                    if (listing.rent < group.value!!.minBudget * group.value!!.size * 1.3) {
                        if (listing.bedrooms > group.value!!.size) {
                            filtered = true
                        }
                    }
                } else {
                    filtered = true
                }
                if (filtered) {
                    ListingItem(
                        address = listing.address,
                        rent = listing.rent,
                        bedrooms = listing.bedrooms,
                        bathrooms = listing.bathrooms,
                        onClick = {
                            navController.navigate("single_listing/${listing.id}")
                        }
                    )
                }
                filtered = false
            }
        }
    }
}
