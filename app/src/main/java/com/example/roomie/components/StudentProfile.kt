package com.example.roomie.components

data class StudentProfile(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val profileType: String = "student",
    val studentUniversity: String = "",
    val studentBasicPreferences: List<String> = emptyList(),
    val studentDesiredGroupSize: List<Int> = listOf(0, 0),
    val studentMaxCommute: Int = 0,
    val studentMaxBudget: Int = 0,
    val studentAge: Int = 0,
    val seenGroupsTimestamps: Map<String, Long> = emptyMap()
)