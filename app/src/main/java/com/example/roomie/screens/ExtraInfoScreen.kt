package com.example.roomie.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.MutableState
import com.example.roomie.components.ProfileTextField

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import com.example.roomie.components.OnboardingProfileState
import com.example.roomie.components.ProfileTextFieldView
import com.example.roomie.components.RoomieTopBar
import com.example.roomie.components.saveProfile
import com.example.roomie.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun ExtraInfoScreen(
    profileState: OnboardingProfileState,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State holders
    val companyField = remember { mutableStateOf(profileState.company) }
    val ageField = remember { mutableStateOf(profileState.age) }
    val universityField = remember { mutableStateOf(profileState.university) }
    val preferencesField = remember { mutableStateOf(profileState.preferences) }
    val groupSizeMinField = remember { mutableStateOf(profileState.groupSizeMin) }
    val groupSizeMaxField = remember { mutableStateOf(profileState.groupSizeMax) }
    val maxCommuteField = remember { mutableStateOf(profileState.maxCommute) }
    val maxBudgetField = remember { mutableStateOf(profileState.maxBudget) }

    val fieldsToValidate: List<MutableState<ProfileTextField>> =
        if (profileState.isLandlord) listOf(companyField)
        else listOf(
            ageField, universityField, preferencesField,
            groupSizeMinField, groupSizeMaxField,
            maxCommuteField, maxBudgetField
        )

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
                Button(onClick = {
                    if (!validateFields(fieldsToValidate)) {
                        Toast.makeText(context, "Please fill all mandatory fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    coroutineScope.launch {
                        val success = saveProfile(profileState)
                        if (success) {
                            Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                            onFinish()
                        } else {
                            Toast.makeText(context, "Error saving profile.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Finish")
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

            if (profileState.isLandlord) {
                item {
                    ProfileTextFieldView(
                        field = companyField.value,
                        onValueChange = {
                            companyField.value.value = it
                            profileState.company.value = it
                        }
                    )
                }
            } else {
                item {
                    ProfileTextFieldView(
                        field = ageField.value,
                        onValueChange = {
                            if (it.isBlank() || (it.all(Char::isDigit) && !it.startsWith("-"))) {
                                ageField.value.value = it
                                profileState.age.value = it
                            }
                        }
                    )
                }

                item {
                    ProfileTextFieldView(
                        field = universityField.value,
                        onValueChange = {
                            universityField.value.value = it
                            profileState.university.value = it
                        }
                    )
                }

                item {
                    ProfileTextFieldView(
                        field = preferencesField.value,
                        onValueChange = {
                            preferencesField.value.value = it
                            profileState.preferences.value = it
                        }
                    )
                }

                item {
                    ProfileTextFieldView(
                        field = groupSizeMinField.value,
                        onValueChange = {
                            if (it.isBlank() || (it.all(Char::isDigit) && !it.startsWith("-"))) {
                                groupSizeMinField.value.value = it
                                profileState.groupSizeMin.value = it
                            }
                        }
                    )
                }

                item {
                    ProfileTextFieldView(
                        field = groupSizeMaxField.value,
                        onValueChange = {
                            if (it.isBlank() || (it.all(Char::isDigit) && !it.startsWith("-"))) {
                                groupSizeMaxField.value.value = it
                                profileState.groupSizeMax.value = it
                            }
                        }
                    )
                }

                item {
                    ProfileTextFieldView(
                        field = maxCommuteField.value,
                        onValueChange = {
                            if (it.isBlank() || (it.all(Char::isDigit) && !it.startsWith("-"))) {
                                maxCommuteField.value.value = it
                                profileState.maxCommute.value = it
                            }
                        }
                    )
                }

                item {
                    ProfileTextFieldView(
                        field = maxBudgetField.value,
                        onValueChange = {
                            if (it.isBlank() || (it.all(Char::isDigit) && !it.startsWith("-"))) {
                                maxBudgetField.value.value = it
                                profileState.maxBudget.value = it
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.extraLong)) } // leave room above bottom bar
        }
    }
}