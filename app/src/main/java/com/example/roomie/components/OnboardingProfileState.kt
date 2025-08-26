package com.example.roomie.components

import androidx.compose.ui.text.input.KeyboardType

// Data class for shared state
data class OnboardingProfileState(
    var name: ProfileTextField = ProfileTextField("Your Name", ""),
    var bio: ProfileTextField = ProfileTextField("About you", ""),
    var phoneNumber: ProfileTextField = ProfileTextField("Phone Number", "", KeyboardType.Phone, false),
    var isLandlord: Boolean = false,

    // Student fields
    var age: ProfileTextField = ProfileTextField("Your Age", "", KeyboardType.Number),
    var university: ProfileTextField = ProfileTextField("Your University", ""),
    var preferences: ProfileTextField = ProfileTextField("Basic Preferences", "", required = false),
    var groupSizeMin: ProfileTextField = ProfileTextField("Min Group Size", "", KeyboardType.Number),
    var groupSizeMax: ProfileTextField = ProfileTextField("Max Group Size", "", KeyboardType.Number),
    var maxCommute: ProfileTextField = ProfileTextField("Max Commute (mins)", "", KeyboardType.Number),
    var maxBudget: ProfileTextField = ProfileTextField("Max Budget (£ / week)", "", KeyboardType.Number),

    // Landlord fields
    var company: ProfileTextField = ProfileTextField("Your Company Name", ""),

    // Lifestyle / extra info
    var smokingStatus: String = "Neither",    // "Smoke" | "Vape" | "Neither"
    var bedtime: String = "",                 // e.g. "11pm–12am"
    var alcoholLevel: Int = 1,                // 1..5
    var pet: String = "No",
    var musicPref: ProfileTextField = ProfileTextField("The type of music I like the most is...", "", required = false),
    var petPeeve: ProfileTextField = ProfileTextField("My biggest pet peeve is...", "", required = false),
    var ideal: ProfileTextField = ProfileTextField("My ideal night is...", "", required = false),
    var addicted: ProfileTextField = ProfileTextField("I am completely addicted to", "", required = false),
    var passionate: ProfileTextField = ProfileTextField("I am passionate about", "", required = false),
)
