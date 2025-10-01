package com.example.roomie.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.theme.FontSize
import com.example.roomie.ui.theme.MontserratFontFamily
import com.example.roomie.ui.theme.Spacing
import com.example.roomie.ui.theme.ZainFontFamily
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifestyleSection(
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
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "Lifestyle",
        fontFamily = MontserratFontFamily,
        fontSize = FontSize.subHeader,
        color = MaterialTheme.colorScheme.inverseSurface
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Smoking / vaping segmented control
    Text(
        "Do you smoke or vape?",
        fontFamily = ZainFontFamily,
        fontSize = FontSize.subHeader,
        color = MaterialTheme.colorScheme.inverseSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("Smoke", "Vape", "Both", "Neither").forEach { option ->
            SegmentedButton(
                selected = smokingStatus.value == option,
                onClick = { smokingStatus.value = option },
                shape = SegmentedButtonDefaults.baseShape,
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveBorderColor = MaterialTheme.colorScheme.surfaceBright,
                    activeContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContainerColor = MaterialTheme.colorScheme.background,
                )
            ) {
                Text(
                    option,
                    style = TextStyle(
                        fontFamily = ZainFontFamily,
                        fontSize = FontSize.subHeader,
                        color = MaterialTheme.colorScheme.inverseSurface
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Bedtime dropdown
    Text(
        "What is your typical bedtime?",
        fontFamily = ZainFontFamily,
        fontSize = FontSize.subHeader,
        color = MaterialTheme.colorScheme.inverseSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    var expanded by remember { mutableStateOf(false) }
    val bedtimeOptions = listOf("<10pm", "10–11pm", "11pm–12am", "12–1am", ">1am")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = bedtimeOptions.getOrNull(bedtime.value - 1) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Select bedtime") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            bedtimeOptions.indices.forEach { index ->
                val option = bedtimeOptions[index]
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        bedtime.value = index + 1
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Alcohol slider 1..5
    Text(
        "How often do you drink alcohol?",
        fontFamily = ZainFontFamily,
        fontSize = FontSize.subHeader,
        color = MaterialTheme.colorScheme.inverseSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(8.dp))
        Slider(
            value = alcoholLevel.value.toFloat(),
            onValueChange = { alcoholLevel.value = it.roundToInt().coerceIn(1,5) },
            valueRange = 1f..5f,
            steps = 3, // 4 steps gives discrete 1..5
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Never",
            fontFamily = ZainFontFamily,
            color = MaterialTheme.colorScheme.inverseSurface,
            fontSize = FontSize.body,
        )
        Text(
            "Very often",
            fontFamily = ZainFontFamily,
            color = MaterialTheme.colorScheme.inverseSurface,
            fontSize = FontSize.body,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // pets
    Text(
        "Do you own a pet?",
        fontFamily = ZainFontFamily,
        fontSize = FontSize.subHeader,
        color = MaterialTheme.colorScheme.inverseSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("Yes", "No").forEach { option ->
            SegmentedButton(
                selected = pet.value == option,
                onClick = { pet.value = option },
                shape = SegmentedButtonDefaults.baseShape,
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveBorderColor = MaterialTheme.colorScheme.surfaceBright,
                    activeContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContainerColor = MaterialTheme.colorScheme.background,
                )
            ) {
                Text(
                    option,
                    style = TextStyle(
                        fontFamily = ZainFontFamily,
                        fontSize = FontSize.subHeader,
                        color = MaterialTheme.colorScheme.inverseSurface
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.medium))

    Text(
        "Beyond the Basics",
        fontFamily = MontserratFontFamily,
        fontSize = FontSize.subHeader,
        color = MaterialTheme.colorScheme.inverseSurface
    )

    Spacer(modifier = Modifier.height(Spacing.extremelyShort))

    // Music preference text
    ProfileTextFieldView(
        field = musicField.value,
        onValueChange = {
            musicField.value.value = it
        }
    )

    Spacer(modifier = Modifier.height(Spacing.extremelyShort))

    // pet peeve preference text
    ProfileTextFieldView(
        field = petPeeve.value,
        onValueChange = {
            petPeeve.value.value = it
        }
    )

    Spacer(modifier = Modifier.height(Spacing.extremelyShort))

    // addicted preference text
    ProfileTextFieldView(
        field = addicted.value,
        onValueChange = {
            addicted.value.value = it
        }
    )

    Spacer(modifier = Modifier.height(Spacing.extremelyShort))

    // ideal preference text
    ProfileTextFieldView(
        field = ideal.value,
        onValueChange = {
            ideal.value.value = it
        }
    )

    Spacer(modifier = Modifier.height(Spacing.extremelyShort))

    // passionate preference text
    ProfileTextFieldView(
        field = passionate.value,
        onValueChange = {
            passionate.value.value = it
        }
    )
}
