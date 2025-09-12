package com.example.roomie.components

data class PreferenceWeights(
    val age: Int = 3,
    val budget: Int = 3,
    val commute: Int = 3,
    val groupSize: Int = 3,
    val bedtime: Int = 3,
    val alcohol: Int = 3,
    val profilePicture: Int = 3,
    val lastSeen: Double = 1.0
) {
    fun toMap(): Map<String, Number> {
        return mapOf(
            "age" to age,
            "budget" to budget,
            "commute" to commute,
            "groupSize" to groupSize,
            "bedtime" to bedtime,
            "alcohol" to alcohol,
            "profilePicture" to profilePicture,
            "lastSeen" to lastSeen
        )
    }
}