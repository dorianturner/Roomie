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
import com.example.roomie.components.listings.getMassCommuteTime
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
    // loading state for filtering listings
    val isLoading = remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()
    val currentUserId = Firebase.auth.currentUser?.uid ?: return

    LaunchedEffect(Unit) {
        try {
            val document = db.collection("users").document(currentUserId).get().await()
            val groupId = if (document.exists()) {
                document.getString("groupId")
            } else {
                null
            }

            val studentMaxBudget = document.getLong("studentMaxBudget")?.toInt() ?: 0
            val studentMaxCommute = document.getLong("studentMaxCommute")?.toInt() ?: 0
            val studentUniversity = document.getString("studentUniversity") ?: ""

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
            if (groupId != null) {
                db.collection("groups")
                    .whereEqualTo("id", groupId)
                    .limit(1) // Only fetch one document
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener

                        if (!snapshot.isEmpty) {
                            val document = snapshot.documents[0]
                            val stats = document.get("stats") as? Map<String, Any>

                            group.value = Group(
                                id = document.id,
                                size = (stats?.get("size") as? Long)?.toInt() ?: 0,
                                minBudget = (stats?.get("minBudget") as? Long)?.toInt() ?: 0,
                                minCommute = (stats?.get("minCommute") as? Long)?.toInt() ?: 0,
                                universities = (stats?.get("universities") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            )
                        }
                        isLoading.value = false
                    }
            } else {
                // dummy group if they are not in a group
                group.value = Group(
                    id = "",
                    size = 1,
                    minBudget = studentMaxBudget,
                    minCommute = studentMaxCommute,
                    universities = List(1) { studentUniversity },
                )
                isLoading.value = false
            }
        } catch (e: Exception) {
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listings") },
            )
        }
    ) { innerPadding ->
        if (isLoading.value) {
            CircularProgressIndicator()
        } else {
            val filteredListings = listings.filter { listing ->
                group.value?.let { currentGroup ->
                    val maxAllowed = currentGroup.minBudget * currentGroup.size * 1.3
                    val budgetOk = listing.rent < maxAllowed
                    val bedroomsOk = listing.bedrooms >= currentGroup.size
                    val commuteOk = getMassCommuteTime(currentGroup.universities, listing.address) <= currentGroup.minCommute
                    budgetOk && bedroomsOk && commuteOk
                } ?: true
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(color = MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(filteredListings) { listing ->
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
}
