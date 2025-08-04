package com.example.roomie.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
    val iconColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = @Composable {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Centered title
                    Text(
                        "Roomie",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 48.dp), // Prevents overlap

                        // Can put in below to give the header it's own colour
                        // color = MaterialTheme.colorScheme.primaryContainer,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Right-aligned actions
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            // Spacing everything here is a bit fiddly might be an easier way to do it
                            .padding(end = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile button
                        IconButton(
                            onClick = onEditProfile,
                            // Change this to change the size of the icon
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Edit Profile",
                                tint = iconColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        // Dropdown menu
                        Box(modifier = Modifier.size(48.dp)) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = iconColor,
                                    // Change this to change the size of the icon
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                // Can add additional dropdown menu items here
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
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Roomie! You are logged in.")
        }
    }
}
