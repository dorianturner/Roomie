package com.example.roomie.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.roomie.components.OnboardingProfileState
import com.example.roomie.components.RoomieTopBar
import com.example.roomie.ui.theme.Spacing

@Composable
fun ProfileTypeScreen(
    profileState: OnboardingProfileState,
    onProfileStateChange: (OnboardingProfileState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { RoomieTopBar() },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.short),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBack) { Text("Back") }
                Button(onClick = onNext) { Text("Next") }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.short),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select Profile Type")

            Spacer(Modifier.height(Spacing.medium))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.short)
            ) {
                Button(
                    onClick = {
                        onProfileStateChange(profileState.copy(isLandlord = false))
                    },
                    colors = if (!profileState.isLandlord) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Student")
                }

                Button(
                    onClick = {
                        onProfileStateChange(profileState.copy(isLandlord = true))
                    },
                    colors = if (profileState.isLandlord) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Landlord")
                }
            }
        }
    }
}