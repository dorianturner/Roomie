package com.example.roomie.components

data class PreferenceWeights(
    val university: Int = 3,
    val budget: Int = 3,
    val commute: Int = 3,
    val groupSize: Int = 3,
    val preferences: Int = 3,
    val lastSeen: Double = 1.0
)