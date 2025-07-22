package com.example.roomie.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileEditorScreen(
    // This callback is triggered when the user saves their profile
    onProfileSaved: () -> Unit
) {
    // Local state to hold the profile data
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Complete Your Profile")
        Spacer(modifier = Modifier.height(32.dp))
        TextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio (Tell us about yourself!)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // In a real app, you would now save this data to Firestore or another database.
                // For example:
                // val currentUser = Firebase.auth.currentUser
                // if (currentUser != null) {
                //     val db = Firebase.firestore
                //     val userProfile = hashMapOf(
                //         "uid" to currentUser.uid,
                //         "displayName" to displayName,
                //         "bio" to bio,
                //         "phoneNumber" to phoneNumber
                //     )
                //     db.collection("users").document(currentUser.uid)
                //         .set(userProfile)
                //         .addOnSuccessListener {
                //             // Data saved successfully, then navigate
                //             onProfileSaved()
                //         }
                //         .addOnFailureListener { e ->
                //             // Handle error, e.g., show a Toast
                //             println("Error saving profile: $e")
                //         }
                // } else {
                //     // User not logged in, handle accordingly
                // }
                // For now, we'll just log and navigate
                println("Profile Saved Locally:")
                println("Display Name: $displayName")
                println("Bio: $bio")
                println("Phone Number: $phoneNumber")

                onProfileSaved() // Trigger navigation to the next screen
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Profile")
        }
    }
}