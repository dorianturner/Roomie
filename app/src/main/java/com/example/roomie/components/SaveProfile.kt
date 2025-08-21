package com.example.roomie.components

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

suspend fun saveProfile(state: OnboardingProfileState): Boolean {
    val auth: FirebaseAuth = Firebase.auth
    val db: FirebaseFirestore = Firebase.firestore

    val currentUser = auth.currentUser
    if (currentUser != null) {
        if (state.name.value.isBlank() ||
            state.bio.value.isBlank()) return false // fails checks
        val data = mutableMapOf<String, Any>(
            "name" to state.name.value,
            "bio" to state.bio.value,
            "phoneNumber" to state.phoneNumber.value,
            "profileType" to if (state.isLandlord) "landlord" else "student",
            "lastUpdated" to System.currentTimeMillis(),
        )

        var isMinProfileSet = true

        if (state.isLandlord) {
            val company = state.company.value
            val name = state.name.value
            data["landlordCompany"] = company
            if (name.isBlank() || company.isBlank()) isMinProfileSet = false
        } else {
            val age = state.age.value.toIntOrNull() ?: -1
            val gMin = state.groupSizeMin.value.toIntOrNull() ?: 0
            val gMax = state.groupSizeMax.value.toIntOrNull() ?: 100
            val commute = state.maxCommute.value.toIntOrNull()
            val budget = state.maxBudget.value.toIntOrNull()

            data["studentAge"] = age
            data["studentUniversity"] = state.university.value
            data["studentDesiredGroupSize"] = listOf(gMin, gMax)
            data["studentMaxCommute"] = commute ?: 999
            data["studentMaxBudget"] = budget ?: 10000
            data["groupId"] = currentUser.uid

            if (
                state.university.value.isBlank() ||
                age < 0
            ) return false // fails checks
        }

        data["minimumRequiredProfileSet"] = isMinProfileSet

        val groupData = mutableMapOf<String, Any>(
            "membersCount" to 1,
            "stats" to mapOf(
                "sumAges" to data["studentAge"]!!,
                "sumBudgets" to data["studentMaxBudget"]!!,
                "sumCommutes" to data["studentMaxCommute"]!!
            )
        )

        return suspendCoroutine { cont ->
            val batch = db.batch()

            val userRef = db.collection("users").document(currentUser.uid)
            val groupRef = db.collection("groups").document(currentUser.uid)

            batch.set(userRef, data)
            batch.set(groupRef, groupData)

            batch.commit()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { cont.resume(false) }
        }
    } else {
        return false
    }
}