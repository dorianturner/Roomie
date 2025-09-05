package com.example.roomie.components.listings

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

data class ListingData(
    val title: String,
    val description: String?,
    val address: String,
    val rent: Int?,
    val bedrooms: Int?,
    val bathrooms: Int?,
    val availableFromEpoch: Long? = null,
    val isActive: Boolean = true
) {
    fun toMap(ownerId: String, ownerName: String?): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "address" to address,
        "rent" to (rent ?: 0),
        "bedrooms" to (bedrooms ?: 0),
        "bathrooms" to (bathrooms ?: 0),
        "availableFrom" to availableFromEpoch,
        "isActive" to isActive,
        "ownerId" to ownerId,
        "ownerName" to ownerName,
        "lastUpdated" to System.currentTimeMillis()
    )
}

/**
 * Save a listing to Firestore.
 * Creates a doc in "listings" (auto-id) and mirrors it under users/{uid}/listings/{listingId}.
 * Returns true on success, false on failure or validation error / not logged in.
 */
suspend fun saveListing(listing: ListingData): Boolean {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val currentUser = auth.currentUser ?: return false

    // Basic validation
    if (listing.title.isBlank()) return false
    if (listing.address.isBlank()) return false
    if (listing.rent == null || listing.rent < 0) return false
    if (listing.bedrooms == null || listing.bedrooms < 0) return false

    val ownerName = try {
        db.collection("users").document(currentUser.uid).get().await().getString("name")
    } catch (e: Exception) {
        null
    }

    return try {
        val batch = db.batch()

        // Main listing document
        val listingRef = db.collection("listings").document()
        batch.set(listingRef, listing.toMap(currentUser.uid, ownerName))

        // Lightweight mirror under user's subcollection
        val userListingRef = db.collection("users").document(currentUser.uid)
            .collection("listings").document(listingRef.id)

        val mirror = mapOf(
            "listingId" to listingRef.id,
            "title" to listing.title,
            "address" to listing.address,
            "rent" to (listing.rent ?: 0),
            "lastUpdated" to System.currentTimeMillis()
        )
        batch.set(userListingRef, mirror)

        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e("SaveListing", "failed saving listing", e)
        false
    }
}