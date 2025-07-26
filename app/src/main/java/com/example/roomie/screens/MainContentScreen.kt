package com.example.roomie.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContentScreen(
    onEditProfile: () -> Unit, // New callback for editing profile
    onLogout: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Roomie") },
                actions = {
                    // Dropdown menu button
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    // The actual dropdown menu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Can add more items to the dropdown menu here as DropdownMenuItem objects
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                showMenu = false
                                Firebase.auth.signOut()
                                onLogout()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Roomie! You are logged in.")
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onEditProfile) { // Button to navigate to profile editor
                Text("Edit My Profile")
            }
        }
    }
}
