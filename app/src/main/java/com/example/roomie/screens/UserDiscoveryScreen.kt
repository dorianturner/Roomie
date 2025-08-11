package com.example.roomie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roomie.components.StudentProfile
import com.example.roomie.components.MatchingService
import android.util.Log

@Composable
fun UserDiscoveryScreen(modifier: Modifier = Modifier) {
    var matches by remember { mutableStateOf<List<StudentProfile>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val matchingService = MatchingService

    LaunchedEffect(Unit) {
        try {
            matches = matchingService.findMatchesForCurrentUser()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            Log.e("UserDiscoveryScreen", "Error fetching matches", e)
            matches = emptyList()
        }
    }

    val currentMatches = matches

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("User Discovery", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        when {
            currentMatches == null -> {
                CircularProgressIndicator()
            }
            errorMessage != null -> {
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
            currentMatches.isEmpty() -> {
                Text("No matches found")
            }
            else -> {
                currentMatches.forEach { profile ->
                    MatchCard(profile)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun MatchCard(profile: StudentProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("University: ${profile.studentUniversity}", style = MaterialTheme.typography.bodyLarge)
            Text("Preferences: ${profile.studentBasicPreferences.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            Text("Desired Group Size: ${profile.studentDesiredGroupSize.joinToString(" - ")}", style = MaterialTheme.typography.bodyMedium)
            Text("Max Commute: ${profile.studentMaxCommute} mins", style = MaterialTheme.typography.bodyMedium)
            Text("Max Budget: $${profile.studentMaxBudget}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
