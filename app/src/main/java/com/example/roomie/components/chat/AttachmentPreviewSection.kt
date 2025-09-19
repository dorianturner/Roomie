package com.example.roomie.components.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AttachmentPreviewSection(
    attachedFiles: List<AttachedFile>,
    onRemoveFile: (Uid) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Attachments (${attachedFiles.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        attachedFiles.forEach { file ->
            AttachmentPreviewItem(
                file = file,
                onRemove = { onRemoveFile(file.uid) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
