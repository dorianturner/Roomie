package com.example.roomie.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainContentScreen(
    onEditProfile: () -> Unit // New callback for editing profile
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to Roomie! You are logged in.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEditProfile) { // Button to navigate to profile editor
            Text("Edit My Profile")
        }
        // You can add a sign out button here later!
    }
}
