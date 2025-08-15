package com.example.roomie.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.roomie.components.OnboardingProfileState
import com.example.roomie.components.ProfileTextFieldView
import com.example.roomie.components.RoomieTopBar
import com.example.roomie.ui.theme.Spacing

@Composable
fun BasicInfoScreen(
    profileState: OnboardingProfileState,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    val nameField = remember { mutableStateOf(profileState.name) }
    val bioField = remember { mutableStateOf(profileState.bio) }
    val phoneNumberField = remember { mutableStateOf(profileState.phoneNumber) }

    Scaffold(
        topBar = @Composable { RoomieTopBar() },
        bottomBar = @Composable {
            Button(
                onClick = {
                    val fieldsToValidate = listOf(nameField, bioField, phoneNumberField)
                    if (validateFields(fieldsToValidate)) {
                        onNext()
                    } else {
                        Toast.makeText(context, "Please fill all mandatory fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.short)
            ) {
                Text("Next")
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
            item {
                Spacer(modifier = Modifier.height(Spacing.long))
            }

            item {
                ProfileTextFieldView(
                    field = nameField.value,
                    onValueChange = {
                        nameField.value.value = it
                        profileState.name.value = it
                    }
                )
            }

            item {
                ProfileTextFieldView(
                    field = bioField.value,
                    onValueChange = {
                        bioField.value.value = it
                        profileState.bio.value = it
                    }
                )
            }

            item {
                ProfileTextFieldView(
                    field = phoneNumberField.value,
                    onValueChange = {
                        if (it.isBlank() || it.all { c -> c.isDigit() || c == '+'}) {
                            phoneNumberField.value.value = it
                            profileState.phoneNumber.value = it
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.short))
            }
        }
    }
}


