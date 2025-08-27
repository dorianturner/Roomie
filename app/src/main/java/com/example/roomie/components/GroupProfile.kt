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