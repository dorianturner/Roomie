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