package com.example.roomie.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Simple reusable field model
data class ProfileTextField(
    val label: String,
    var value: String,
    val keyboardType: KeyboardType = KeyboardType.Text
)

// --- Generic TextField Renderer ---
@Composable
fun ProfileTextFieldView(
    field: ProfileTextField,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = field.value,
        onValueChange = onValueChange,
        label = { Text(field.label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType)
    )
}

// --- Landlord Profile Section ---
@Composable
fun LandlordProfileSection(
    companyField: ProfileTextField,
    onCompanyChange: (String) -> Unit
) {
    Text("Landlord Profile", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    ProfileTextFieldView(
        field = companyField,
        onValueChange = onCompanyChange
    )
}

// --- Student Profile Section ---
@Composable
fun StudentProfileSection(
    ageField: ProfileTextField,
    universityField: ProfileTextField,
    preferencesField: ProfileTextField,
    groupSizeMinField: ProfileTextField,
    groupSizeMaxField: ProfileTextField,
    maxCommuteField: ProfileTextField,
    maxBudgetField: ProfileTextField,
    onFieldChange: (ProfileTextField, String) -> Unit
) {
    Column {
        Text("Student Profile", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = ageField,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    onFieldChange(ageField, newValue)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(field = universityField, onValueChange = {
            onFieldChange(universityField, it)
        })
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(field = preferencesField, onValueChange = {
            onFieldChange(preferencesField, it)
        })
        Spacer(modifier = Modifier.height(16.dp))

        Text("Desired Group Size (Min-Max):", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileTextFieldView(
                field = groupSizeMinField,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        onFieldChange(groupSizeMinField, newValue)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Text("-", modifier = Modifier.alignByBaseline())
            ProfileTextFieldView(
                field = groupSizeMaxField,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        onFieldChange(groupSizeMaxField, newValue)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = maxCommuteField,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    onFieldChange(maxCommuteField, newValue)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = maxBudgetField,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    onFieldChange(maxBudgetField, newValue)
                }
            }
        )
    }
}
