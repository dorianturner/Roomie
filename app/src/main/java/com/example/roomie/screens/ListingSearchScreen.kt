package com.example.roomie.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.roomie.components.listings.Group
import com.example.roomie.components.listings.Listing
import com.example.roomie.components.listings.ListingItem
import com.example.roomie.components.listings.getMassCommuteTime
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySearchScreen(
    navController: NavController,
) {

    val listings = remember { mutableStateListOf<Listing>() }
    val group = remember { mutableStateOf<Group?>(null) }

    // states for filtering listings
    val filteredListings = remember { mutableStateListOf<Listing>() }
    val isFiltering = remember { mutableStateOf(false) }
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
                            val statsRaw = document.get("stats") as? Map<*, *>
                            val stats = statsRaw?.mapNotNull {
                                val key = it.key as? String
                                val value = it.value
                                if (key != null) key to value else null
                            }?.toMap()

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
        } catch (_: Exception) {
            isLoading.value = false
        }
    }

    // filtering listings
    LaunchedEffect(listings, group.value) {
        if (listings.isNotEmpty() && group.value != null && !isLoading.value) {
            isFiltering.value = true
            filteredListings.clear()

            val currentGroup = group.value!!
            val maxAllowed = currentGroup.minBudget * currentGroup.size * 1.3

            // first filter by params that don't need async calls
            val preFilteredListings = listings.filter { listing ->
                val budgetOk = listing.rent < maxAllowed
                val bedroomsOk = listing.bedrooms >= currentGroup.size
                budgetOk && bedroomsOk
            }

            // filter by commute time
            for (listing in preFilteredListings) {
                try {
                    val commuteTime = getMassCommuteTime(currentGroup.universities, listing.address)
                    if (commuteTime <= currentGroup.minCommute) {
                        filteredListings.add(listing)
                    }
                } catch (e: Exception) {
                    Log.e("PropertySearchScreen", "Error calculating commute time for ${listing.address}.", e)
                }
            }
            isFiltering.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listings") },
            )
        }
    ) { innerPadding ->
        if (isLoading.value || isFiltering.value) {
            CircularProgressIndicator()
        } else {
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
                        displayImages = listing.photos,
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
