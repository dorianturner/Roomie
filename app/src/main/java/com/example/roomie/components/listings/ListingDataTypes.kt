package com.example.roomie.components.listings

import com.example.roomie.components.PhotoItem
import com.google.firebase.firestore.PropertyName

data class Listing(
    val ownerId: String = "",
    val id: String = "",
    val title: String = "",
    val address: String = "",
    val description: String = "",
    @get:PropertyName("rent")
    val rent: Int = 0,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val availableFrom: Long? = null,
    val isActive: Boolean = true,
    val photos: List<PhotoItem> = emptyList()
)

// may be possible to extract this for general use with user discovery
// this isn't storing every field of the group, only the ones relevant for filtering the listings
data class Group(
    val id: String = "",
    val minBudget: Int = 0,
    val size: Int = 0,
    val minCommute: Int = 0,
    val universities: List<String> = emptyList()
)
