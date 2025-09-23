package com.example.roomie.components

/**
 * Represents the profile of a student user.
 *
 * @property id The unique identifier for the student. Defaults to an empty string.
 * @property name The name of the student. Defaults to an empty string.
 * @property photos A list of URLs or identifiers for the student's photos. Defaults to an empty list.
 * @property studentAge The age of the student. Nullable.
 * @property profilePictureUrl The URL or identifier for the student's main profile picture. Nullable.
 * @property studentPet The student's preference or information regarding pets. Nullable.
 * @property studentBedtime The student's preferred bedtime, represented as an Int (e.g., 1 for <10pm, 5 for >1am). Nullable.
 * @property studentAlcohol The student's preference or habits regarding alcohol consumption, possibly an integer scale. Nullable.
 * @property studentSmokingStatus The student's smoking status (e.g., "non-smoker", "smoker"). Nullable.
 * @property groupMin The minimum desired group size for housing. Nullable.
 * @property groupMax The maximum desired group size for housing. Nullable.
 * @property studentMaxCommute The maximum acceptable commute time for the student, typically in minutes. Nullable.
 * @property studentMaxBudget The maximum budget for housing. Nullable.
 * @property studentUniversity The university the student attends. Nullable.
 * @property bio A short biography or description provided by the student. Nullable.
 * @property studentAddicted Things the student is "addicted to" or enjoys a lot (e.g., hobbies, interests). Nullable.
 * @property studentPetPeeve The student's pet peeves. Nullable.
 * @property passionate Things the student is passionate about. Nullable.
 * @property studentIdeal The student's description of an ideal roommate or living situation. Nullable.
 * @property studentMusic The student's music preferences. Nullable.
 * @property phoneNumber The student's phone number. Nullable.
 * @property seenUsersTimestamps A map tracking when other users/groups were last seen in discovery, to manage visibility or decay scores. Defaults to an empty map.
 */
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
    /**
     * Converts the [StudentProfile] object into a [Map] representation.
     * This is useful for storing the profile data in a database (like Firestore)
     * or for sending it over a network.
     *
     * @return A map where keys are the property names (String) and values are their corresponding values.
     *         The `id` field is intentionally excluded as it's often used as the document ID in Firestore.
     */
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
