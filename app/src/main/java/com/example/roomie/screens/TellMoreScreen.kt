package com.example.roomie.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.roomie.components.LifestyleSection
import com.example.roomie.components.OnboardingProfileState
import com.example.roomie.components.RoomieTopBar
import com.example.roomie.components.saveProfile
import com.example.roomie.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun TellMoreScreen(
    profileState: OnboardingProfileState,
    onNext: () -> Unit,   // call when onboarding fully finished
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // local UI state initialised from profileState
    val smoking = remember { mutableStateOf(profileState.smokingStatus) }
    val bedtime = remember { mutableStateOf(profileState.bedtime) }
    val alcohol = remember { mutableStateOf(profileState.alcoholLevel) } // MutableState<Int>
    val music = remember { mutableStateOf(profileState.musicPref) }

    var isSaving = remember { mutableStateOf(false) }

    Scaffold(
        topBar = { RoomieTopBar() },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.short),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { if (!isSaving.value) onBack() }, enabled = !isSaving.value) {
                    Text("Back")
                }

                Button(onClick = {
                    if (isSaving.value) return@Button

                    // Validate mandatory lifestyle fields
                    if (smoking.value.isBlank() || bedtime.value.isBlank() || alcohol.value !in 1..5) {
                        Toast.makeText(context, "Please fill smoking, bedtime and drinking preferences", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // copy local selections into the shared profile state
                    profileState.smokingStatus = smoking.value
                    profileState.bedtime = bedtime.value
                    profileState.alcoholLevel = alcohol.value
                    profileState.musicPref = music.value

                    // perform final save
                    coroutineScope.launch {
                        isSaving.value = true
                        try {
                            val ok = saveProfile(profileState)
                            if (ok) {
                                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                                onNext()
                            } else {
                                Toast.makeText(context, "Error saving profile. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving.value = false
                        }
                    }
                }, enabled = !isSaving.value) {
                    Text(if (isSaving.value) "Saving..." else "Finish")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.short),
            verticalArrangement = Arrangement.spacedBy(Spacing.short),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(Spacing.medium)) }

            item {
                LifestyleSection(
                    smokingStatus = smoking,
                    bedtime = bedtime,
                    alcoholLevel = alcohol,
                    musicField = music
                )
            }

            item { Spacer(modifier = Modifier.height(Spacing.extraLong)) }
        }
    }
}
