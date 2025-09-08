package com.example.roomie.components.listings

data class Listing(
    val id: String = "",
    val title: String = "",
    val address: String = "",
    val description: String = "",
    val rent: Int = 0,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val availableFrom: Long? = null,
    val isActive: Boolean = true
)

// may be possible to extract this for general use with user discovery
// this isn't storing every field of the group, only the ones relevant for filtering the listings
data class Group(
    val id: String = "",
    val minBudget: Long = 0,
    val maxBudget: Long = 0,
    val size: Long = 0,
    val minCommute: Long = 0,
    val maxCommute: Long = 0,
)