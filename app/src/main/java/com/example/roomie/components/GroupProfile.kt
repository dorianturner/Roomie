package com.example.roomie.components

data class GroupProfile(
    val id: String,
    val name: String,
    val members: List<StudentProfile>,
    val stats: GroupStats
)

data class GroupStats(
    val size: Int,
    val avgBudget: Int,
    val avgCommute: Int,
    val avgAge: Int,
)

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