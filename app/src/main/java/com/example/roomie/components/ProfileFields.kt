package com.example.roomie.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.MutableState

// Simple reusable field model
data class ProfileTextField(
    val label: String,
    var value: String,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val required: Boolean = true,
    val isError: Boolean = false
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
        label = {
            Text(
                if (!field.required) "${field.label} (optional)" else field.label
            )
        },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
        isError = field.isError,
        supportingText = {
            if (field.isError) {
                Text(text = "This field is required", color = MaterialTheme.colorScheme.error)
            }
        }
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
    ageField: MutableState<ProfileTextField>,
    universityField: MutableState<ProfileTextField>,
    preferencesField: MutableState<ProfileTextField>,
    groupSizeMinField: MutableState<ProfileTextField>,
    groupSizeMaxField: MutableState<ProfileTextField>,
    maxCommuteField: MutableState<ProfileTextField>,
    maxBudgetField: MutableState<ProfileTextField>
) {
    Column {
        Text("Student Profile", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = ageField.value,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    ageField.value = ageField.value.copy(value = newValue)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = universityField.value,
            onValueChange = {
                universityField.value = universityField.value.copy(value = it)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = preferencesField.value,
            onValueChange = {
                preferencesField.value = preferencesField.value.copy(value = it)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Desired Group Size (Min-Max):", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileTextFieldView(
                field = groupSizeMinField.value,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        groupSizeMinField.value = groupSizeMinField.value.copy(value = newValue)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Text("-", modifier = Modifier.alignByBaseline())
            ProfileTextFieldView(
                field = groupSizeMaxField.value,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        groupSizeMaxField.value = groupSizeMaxField.value.copy(value = newValue)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = maxCommuteField.value,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    maxCommuteField.value = maxCommuteField.value.copy(value = newValue)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(
            field = maxBudgetField.value,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    maxBudgetField.value = maxBudgetField.value.copy(value = newValue)
                }
            }
        )
    }
}
