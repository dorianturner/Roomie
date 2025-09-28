package com.example.roomie.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.theme.FontSize
import com.example.roomie.ui.theme.MontserratFontFamily

@Composable
fun LogoutAlertDialog(
    onDismiss:() -> Unit,
    onLogout:() -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onDismiss()
                           },
        title = {
            Text(
                text = "Log out?",
                style = TextStyle(
                    fontFamily = MontserratFontFamily,
                    fontSize = FontSize.header,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to log out?",
                style = TextStyle(
                    fontFamily = MontserratFontFamily,
                    fontSize = FontSize.subHeader,
                ),
                color = MaterialTheme.colorScheme.primary
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onLogout()
            }) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            })
            {
                Text("No")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.surfaceBright,
        textContentColor = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(40.dp)
    )
}
