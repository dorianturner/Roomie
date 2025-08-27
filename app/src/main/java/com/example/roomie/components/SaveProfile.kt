package com.example.roomie.components

import android.util.Log
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
        if (state.name.value.isBlank()) return false // fails checks
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

            // mandatory lifestyle checks
            if (state.smokingStatus.isBlank() || state.bedtime !in 1..5) return false
            if (state.alcoholLevel !in 1..5) return false

            data["studentAge"] = age
            data["studentUniversity"] = state.university.value
            data["studentDesiredGroupSize"] = listOf(gMin, gMax)
            data["studentMaxCommute"] = commute ?: 999
            data["studentMaxBudget"] = budget ?: 10000
            data["groupId"] = currentUser.uid

            // persist lifestyle
            data["studentSmokingStatus"] = state.smokingStatus
            data["studentBedtime"] = state.bedtime
            data["studentAlcohol"] = state.alcoholLevel
            data["studentPet"] = state.pet
            data["studentMusic"] = state.musicPref.value
            data["studentPetPeeve"] = state.petPeeve.value
            data["studentAddicted"] = state.addicted.value
            data["studentIdeal"] = state.ideal.value
            data["studentPassionate"] = state.passionate.value

            if (
                state.university.value.isBlank() ||
                age < 0
            ) return false // fails checks
        }

        data["minimumRequiredProfileSet"] = isMinProfileSet


        return suspendCoroutine { cont ->
            val batch = db.batch()

            val userRef = db.collection("users").document(currentUser.uid)

            batch.set(userRef, data)

            // temp fix while landlords arent there
            if (!state.isLandlord) {
                val groupRef = db.collection("groups").document(currentUser.uid)

                val members: List<StudentProfile> = listOf(StudentProfile(
                    id = currentUser.uid,
                    name = data["name"] as String,
                    studentAge = data["studentAge"] as Int?,
                    photos = listOf(),
                    profilePictureUrl = data["profilePictureUrl"] as String?,
                    studentPet = data["studentPet"] as String?,
                    studentBedtime = data["studentBedtime"] as Int?,
                    studentAlcohol = data["studentAlcohol"] as Int?,
                    studentSmokingStatus = data["studentSmokingStatus"] as String?,
                    groupMin = (data["studentDesiredGroupSize"] as List<Int>?)?.getOrNull(0),
                    groupMax = (data["studentDesiredGroupSize"] as List<Int>?)?.getOrNull(1),
                    studentMaxCommute = data["studentMaxCommute"] as Int?,
                    studentMaxBudget = data["studentMaxBudget"] as Int?,
                    studentUniversity = data["studentUniversity"] as String?,
                    bio = data["bio"] as String?,
                    studentAddicted = data["studentAddicted"] as String?,
                    studentPetPeeve = data["studentPetPeeve"] as String?,
                    passionate = data["studentPassionate"] as String?,
                    studentIdeal = data["studentIdeal"] as String?,
                    studentMusic = data["studentMusic"] as String?,
                    phoneNumber = data["phoneNumber"] as String?,
                )) // just the new user
                val stats = generateGroupStats(members)

                val groupProfile = GroupProfile(
                    id = currentUser.uid,
                    name = data["name"] as String,
                    members = members,
                    stats = stats
                )

                batch.set(groupRef, groupProfile.toMap())

                FunctionsProvider.instance
                    .getHttpsCallable("upsertGroupProfile")
                    .call(groupProfile.toMap())
                    .addOnSuccessListener { result ->
                        Log.d("SaveProfile","Group upsert success: ${result.data}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("SaveProfile","Group upsert failure", e)
                    }
            }

            batch.commit()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { cont.resume(false) }
        }
    } else {
        return false
    }
}