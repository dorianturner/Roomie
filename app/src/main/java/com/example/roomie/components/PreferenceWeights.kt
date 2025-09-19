package com.example.roomie.components

/**
 * Data class representing the weights assigned to various preferences for a matching algorithm.
 * These weights determine the importance of each factor when calculating match scores.
 *
 * @property age The weight for age preference. Defaults to 3.
 * @property budget The weight for budget preference. Defaults to 3.
 * @property commute The weight for commute time preference. Defaults to 3.
 * @property groupSize The weight for preferred group size. Defaults to 3.
 * @property bedtime The weight for bedtime preference. Defaults to 3.
 * @property alcohol The weight for alcohol consumption preference. Defaults to 3.
 * @property profilePicture The weight for the presence of a profile picture. Defaults to 3.
 * @property lastSeen The weight for how recently a user/group was last seen or active. Defaults to 1.0.
 */
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
    /**
     * Converts the [PreferenceWeights] object to a [Map] representation.
     * This is useful for storing the weights in a database or sending them over a network.
     *
     * @return A map where keys are preference names (String) and values are their weights (Number).
     */
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
