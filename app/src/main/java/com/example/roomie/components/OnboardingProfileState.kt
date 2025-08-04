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
    var preferences: ProfileTextField = ProfileTextField("Basic Preferences", ""),
    var groupSizeMin: ProfileTextField = ProfileTextField("Min Group Size", "", KeyboardType.Number, false),
    var groupSizeMax: ProfileTextField = ProfileTextField("Max Group Size", "", KeyboardType.Number, false),
    var maxCommute: ProfileTextField = ProfileTextField("Max Commute (mins)", "", KeyboardType.Number, false),
    var maxBudget: ProfileTextField = ProfileTextField("Max Budget (£ / week)", "", KeyboardType.Number, false),

    // Landlord fields
    var company: ProfileTextField = ProfileTextField("Your Company Name", "")
)
