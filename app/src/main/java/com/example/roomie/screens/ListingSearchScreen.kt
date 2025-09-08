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

    LaunchedEffect(Unit) {
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
            // this members field may not be exactly how data is structured in the database
            .whereArrayContains("members", currentUserId)
            .limit(1) // Only fetch one document
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                if (!snapshot.isEmpty) {
                    val document = snapshot.documents[0]
                    group.value = document.toObject(Group::class.java)?.copy(id = document.id)
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
            items(listings) { listing ->
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
        }
    }
}
