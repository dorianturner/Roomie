package com.example.roomie.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.More
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.Navigator
import com.example.roomie.components.NavigationBarItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContentScreen(
    onEditProfile: () -> Unit, // New callback for editing profile
    onLogout: () -> Unit,
    onChats: () -> Unit,
    onBookmarks: () -> Unit,
    onPropertySearch: () -> Unit,
    onOptions: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val iconColor = MaterialTheme.colorScheme.primary

    val navBarItemList = listOf(
        NavigationBarItem("Chat", Icons.AutoMirrored.Filled.Chat, onChats),
        NavigationBarItem("Bookmarks", Icons.Default.Favorite, onBookmarks),
        NavigationBarItem("Search", Icons.Default.Search, onPropertySearch),
        NavigationBarItem("Profile", Icons.Default.Person, onEditProfile),
        NavigationBarItem("Options", Icons.Default.MoreVert, onOptions)
    )

    var selectedPage by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        bottomBar = @Composable {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    navBarItemList.forEachIndexed { index, navigationItem ->
                        NavigationBarItem(
                            selected = selectedPage == index,
                            onClick = {
                                selectedPage = index
                                navigationItem.onClick()
                            },
                            label = {
                                Text(
                                    text = navigationItem.label,
                                    style = TextStyle(
                                        fontSize = 12.sp
                                    )
                                )
                                    },
                            icon = {
                                Icon(
                                    imageVector = navigationItem.icon,
                                    contentDescription = navigationItem.label,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.surfaceBright,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.surfaceBright,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        topBar = @Composable {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Centered title
                    Text(
                        text = "Roomie",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 48.dp), // Prevents overlap

                        // Can put in below to give the header it's own colour
                        // color = MaterialTheme.colorScheme.primaryContainer,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
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
            Text(
                text = "Welcome to Roomie! You are logged in.",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

//@Composable
//fun ContentScreen(modifier: Modifier = Modifier, selectedIndex : Int) {
//    when(selectedIndex){
//        0 -> ,
//        1 -> ,
//        2 -> ,
//        3 -> ProfileEditorScreen()
//    }
//}
