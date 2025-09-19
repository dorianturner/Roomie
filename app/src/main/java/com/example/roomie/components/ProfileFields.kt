package com.example.roomie.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Simple reusable field model
class ProfileTextField(
    val label: String,
    value: String = "",
    val keyboardType: KeyboardType = KeyboardType.Text,
    val required: Boolean = true,
    private val validator: ((String) -> Boolean)? = null
) {
    var value by mutableStateOf(value)
    var isError by mutableStateOf(false)

    fun validate(): Boolean {
        val isValid = if (required) {
            value.isNotBlank() && (validator?.invoke(value) ?: true)
        } else {
            value.isBlank() || (validator?.invoke(value) ?: true)
        }
        isError = !isValid
        return isValid
    }
}

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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
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
    groupSizeMinField: MutableState<ProfileTextField>,
    groupSizeMaxField: MutableState<ProfileTextField>,
    maxCommuteField: MutableState<ProfileTextField>,
    maxBudgetField: MutableState<ProfileTextField>,
    smokingStatus: MutableState<String>,
    bedtime: MutableState<Int>,
    alcoholLevel: MutableState<Int>,
    pet: MutableState<String>,
    musicField: MutableState<ProfileTextField>,
    petPeeve: MutableState<ProfileTextField>,
    addicted: MutableState<ProfileTextField>,
    ideal: MutableState<ProfileTextField>,
    passionate: MutableState<ProfileTextField>,
) {
    Column {
        Text("Student Profile", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // ... existing fields (age/university/group size/commute/budget) - unchanged ...
        ProfileTextFieldView(field = ageField.value, onValueChange = { newValue ->
            if (newValue.all { it.isDigit() } || newValue.isEmpty()) ageField.value.value = newValue
        })
        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextFieldView(field = universityField.value, onValueChange = { universityField.value.value = it })
        Spacer(modifier = Modifier.height(16.dp))

        // group size row...
        Text(
            "Desired Group Size (Min-Max):",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically, // ensures the dash is centered with text fields
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileTextFieldView(
                field = groupSizeMinField.value,
                onValueChange = { v ->
                    if (v.all { it.isDigit() } || v.isEmpty()) groupSizeMinField.value.value = v
                },
                modifier = Modifier.weight(1f)
            )

            Text("-", style = MaterialTheme.typography.bodyLarge) // optional style for consistency

            ProfileTextFieldView(
                field = groupSizeMaxField.value,
                onValueChange = { v ->
                    if (v.all { it.isDigit() } || v.isEmpty()) groupSizeMaxField.value.value = v
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileTextFieldView(field = maxCommuteField.value, onValueChange = { v ->
            if (v.all { it.isDigit() } || v.isEmpty()) maxCommuteField.value.value = v
        })
        Spacer(modifier = Modifier.height(16.dp))
        ProfileTextFieldView(field = maxBudgetField.value, onValueChange = { v ->
            if (v.all { it.isDigit() } || v.isEmpty()) maxBudgetField.value.value = v
        })

        // Lifestyle section (smoking/bedtime/alcohol/music)
        LifestyleSection(
            smokingStatus = smokingStatus,
            bedtime = bedtime,
            alcoholLevel = alcoholLevel,
            pet = pet,
            musicField = musicField,
            petPeeve = petPeeve,
            addicted = addicted,
            ideal = ideal,
            passionate = passionate
        )
    }
}
