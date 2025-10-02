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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.theme.FontSize
import com.example.roomie.ui.theme.ZainFontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.roomie.ui.theme.MontserratFontFamily
import com.example.roomie.ui.theme.Spacing


/**
 * A reusable model for a profile text field, managing its state and validation.
 *
 * @property label The text label displayed for the field.
 * @param value The initial value of the text field. Defaults to an empty string.
 * @property keyboardType The type of keyboard to be shown for this field (e.g., Text, Number).
 * @property required Indicates if the field must be filled. Defaults to true.
 * @param validator An optional lambda function to perform custom validation on the field's value.
 *                  It should return `true` if the value is valid, `false` otherwise.
 */
class ProfileTextField(
    val label: String,
    value: String = "",
    val keyboardType: KeyboardType = KeyboardType.Text,
    val required: Boolean = true,
    private val validator: ((String) -> Boolean)? = null
) {
    /** The current value of the text field, observable by Compose. */
    var value by mutableStateOf(value)
    /** Indicates if the current value of the field is invalid, observable by Compose. */
    var isError by mutableStateOf(false)

    /**
     * Validates the current value of the text field.
     * If `required` is true, the field must not be blank and must pass the custom `validator` if provided.
     * If `required` is false, the field is valid if it's blank or if it passes the custom `validator` if provided.
     * Sets the [isError] state based on the validation result.
     *
     * @return `true` if the field's value is valid, `false` otherwise.
     */
    fun validate(): Boolean {
        val isValid = if (required) {
            value.isNotBlank() && (validator?.invoke(value) ?: true)
        } else {
            // Field is not required, so it's valid if blank OR if it passes the validator
            value.isBlank() || (validator?.invoke(value) ?: true)
        }
        isError = !isValid
        return isValid
    }
}

/**
 * A composable function that renders a generic TextField based on a [ProfileTextField] model.
 * It displays the label, handles value changes, and shows error states.
 *
 * @param field The [ProfileTextField] model instance that provides the data and state for this view.
 * @param onValueChange A callback function that is invoked when the text field's value changes.
 * @param modifier Optional [Modifier] for customizing the layout and appearance of the TextField.
 */
@Composable
fun ProfileTextFieldView(
    field: ProfileTextField,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val labelText = buildAnnotatedString {
        withStyle(
            style = SpanStyle(color = MaterialTheme.colorScheme.surfaceTint)
        ) {
            append(field.label)
        }
        if (!field.required) {
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.onTertiary)
            ) {
                append(" (optional)")
            }
        }
    }
    OutlinedTextField(
        value = field.value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = labelText,
                style = TextStyle(
                    fontFamily = ZainFontFamily,
                    fontSize = FontSize.body,
                    color = MaterialTheme.colorScheme.surfaceTint
                )
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceBright,
            errorBorderColor = Color.Red,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),

        shape = RoundedCornerShape(12.dp),

        textStyle = TextStyle(
            fontFamily = ZainFontFamily,
            fontSize = FontSize.subHeader,
            color = MaterialTheme.colorScheme.inverseSurface,
        ),

        keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
        isError = field.isError,
        supportingText = {
            if (field.isError) {
                Text(text = "This field is required", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

/**
 * A composable function that displays the profile section specific to landlords.
 * Currently, this includes a field for the company name.
 *
 * @param companyField The [ProfileTextField] model for the company name input.
 * @param onCompanyChange A callback function invoked when the company name field's value changes.
 */
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

/**
 * A composable function that displays the profile section specific to students.
 * It includes various fields for student details like age, university, group size preferences,
 * commute time, budget, and lifestyle choices.
 *
 * @param ageField Mutable state holding the [ProfileTextField] for the student's age.
 * @param universityField Mutable state holding the [ProfileTextField] for the student's university.
 * @param groupSizeMinField Mutable state holding the [ProfileTextField] for the minimum desired group size.
 * @param groupSizeMaxField Mutable state holding the [ProfileTextField] for the maximum desired group size.
 * @param maxCommuteField Mutable state holding the [ProfileTextField] for the maximum commute time.
 * @param maxBudgetField Mutable state holding the [ProfileTextField] for the maximum budget.
 * @param smokingStatus Mutable state holding the student's smoking status string.
 * @param bedtime Mutable state holding the student's preferred bedtime (represented as an Int, e.g., hours).
 * @param alcoholLevel Mutable state holding the student's alcohol consumption preference (represented as an Int).
 * @param pet Mutable state holding the student's pet preference string.
 * @param musicField Mutable state holding the [ProfileTextField] for the student's music preferences.
 * @param petPeeve Mutable state holding the [ProfileTextField] for the student's pet peeves.
 * @param addicted Mutable state holding the [ProfileTextField] for things the student is "addicted" to or enjoys a lot.
 * @param ideal Mutable state holding the [ProfileTextField] for the student's description of an ideal roommate/living situation.
 * @param passionate Mutable state holding the [ProfileTextField] for things the student is passionate about.
 */
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
        Text(
            "Student Profile",
            fontFamily = MontserratFontFamily,
            fontSize = FontSize.header,
            color = MaterialTheme.colorScheme.inverseSurface,

        )
        Spacer(modifier = Modifier.height(Spacing.extremelyShort))

        // ... existing fields (age/university/group size/commute/budget) - unchanged ...
        ProfileTextFieldView(field = ageField.value, onValueChange = { newValue ->
            if (newValue.all { it.isDigit() } || newValue.isEmpty()) ageField.value.value = newValue
        })
        Spacer(modifier = Modifier.height(Spacing.extremelyShort))

        ProfileTextFieldView(field = universityField.value, onValueChange = { universityField.value.value = it })

        Spacer(modifier = Modifier.height(Spacing.extremelyShort))
        ProfileTextFieldView(field = maxCommuteField.value, onValueChange = { v ->
            if (v.all { it.isDigit() } || v.isEmpty()) maxCommuteField.value.value = v
        })
        Spacer(modifier = Modifier.height(Spacing.extremelyShort))
        ProfileTextFieldView(field = maxBudgetField.value, onValueChange = { v ->
            if (v.all { it.isDigit() } || v.isEmpty()) maxBudgetField.value.value = v
        })

        Spacer(modifier = Modifier.height(Spacing.short))

        // group size row...
        Text(
            "Desired Group Size (Min-Max):",
            fontFamily = MontserratFontFamily,
            fontSize = FontSize.body,
            color = MaterialTheme.colorScheme.inverseSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically, // ensures the dash is centered with text fields
            horizontalArrangement = Arrangement.spacedBy(Spacing.extremelyShort),
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
