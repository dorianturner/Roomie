package com.example.roomie.components

data class StudentProfile(
    val id: String = "",
    val name: String = "",
    val photos: List<String> = emptyList(),
    val studentAge: Int? = null,
    val profilePictureUrl: String?= null,
    val studentPet: String? = null,
    val studentBedtime: Int? = null, // instead gonna store this as Int, 1 = <10pm, 5 = >1am
    val studentAlcohol: Int? = null,
    val studentSmokingStatus: String? = null,
    val groupMin: Int? = null,
    val groupMax: Int? = null,
    val studentMaxCommute: Int? = null,
    val studentMaxBudget: Int? = null,
    val studentUniversity: String? = null,
    val bio: String? = null,
    val studentAddicted: String? = null,
    val studentPetPeeve: String? = null,
    val passionate: String? = null,
    val studentIdeal: String? = null,
    val studentMusic: String? = null,
    val phoneNumber: String? = null,
    val seenUsersTimestamps: Map<String, Long> = emptyMap()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "photos" to photos,
            "studentAge" to studentAge,
            "profilePictureUrl" to profilePictureUrl,
            "studentPet" to studentPet,
            "studentBedtime" to studentBedtime,
            "studentSmokingStatus" to studentSmokingStatus,
            "groupMin" to groupMin,
            "groupMax" to groupMax,
            "studentMaxCommute" to studentMaxCommute,
            "studentMaxBudget" to studentMaxBudget,
            "studentUniversity" to studentUniversity,
            "bio" to bio,
            "studentAddicted" to studentAddicted,
            "studentPetPeeve" to studentPetPeeve,
            "passionate" to passionate,
            "studentIdeal" to studentIdeal,
            "studentMusic" to studentMusic,
            "studentAlcohol" to studentAlcohol,
            "seenUsersTimestamps" to seenUsersTimestamps,
        )
    }
}
