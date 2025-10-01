package com.example.roomie.components.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roomie.components.GroupProfile
import com.example.roomie.components.ProfileCard
import com.example.roomie.components.StudentProfile
import com.example.roomie.ui.theme.Spacing

const val POPUP_HEIGHT_RATIO = 0.9f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOnTap(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    groupProfile: GroupProfile
) {
    val scrollState = rememberScrollState()
    val isIndividual: Boolean = groupProfile.stats.size == 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.inverseSurface
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(POPUP_HEIGHT_RATIO)
                .verticalScroll(scrollState)
                .padding(Spacing.short)
        ) {
            if (isIndividual) {
                val student = groupProfile.members.firstOrNull()
                if (student != null) {
                    IndividualProfilePopUp(student)
                }
            } else {
                GroupProfilePopUp(groupProfile)
            }
        }
    }
}

@Composable
fun IndividualProfilePopUp(studentProfile: StudentProfile) {
    ProfileCard(
        studentProfile = studentProfile,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun GroupProfilePopUp(groupProfile: GroupProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = groupProfile.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        groupProfile.members.forEach { member ->
            ProfileCard(
                studentProfile = member,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
