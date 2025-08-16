package com.example.roomie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import com.example.roomie.components.NavigationBarItem
import com.example.roomie.components.LogoutAlertDialog
import com.example.roomie.components.RoomieNameLogo
import com.example.roomie.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContentScreen(
    onEditProfile: () -> Unit, // New callback for editing profile
    onNavigateToChat: ()-> Unit,
    onLogout: () -> Unit,
) {

    var showMenu by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    val navBarItemList = listOf(
        NavigationBarItem("Chats", Icons.AutoMirrored.Filled.Chat),
        NavigationBarItem("Bookmarks", Icons.Default.Favorite),
        NavigationBarItem("Search", Icons.Default.Search),
        NavigationBarItem("Profile", Icons.Default.Person),
    )

    var selectedPage by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars),
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
                                    modifier = Modifier.size(Spacing.long)
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
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (showLogoutConfirmation) {
                        LogoutAlertDialog(
                            onDismiss = { showLogoutConfirmation = false },
                            onLogout = {
                                Firebase.auth.signOut()
                                onLogout()
                            }
                        )
                    }
                    // Centered title
                    RoomieNameLogo(
                        modifier = Modifier
                            .width(150.dp)
                            .align(Alignment.Center)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.surfaceBright,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(x = (-10).dp, y = 7.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary)
                        ) {
                            // Can add additional dropdown menu items here
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Log out",
                                        // Probably should not be hard-coded, can make separate class when new items are added
                                        modifier = Modifier.padding(horizontal = 5.dp),
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                       },
                                onClick = {
                                    showMenu = false
                                    showLogoutConfirmation = true
                                }
                            )
                        }
                    }
                }

                // The chat button
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(vertical = 20.dp, horizontal = 20.dp),
                    ) {
                        Icon(
                            // will need to dig around for a better icon
                            imageVector = Icons.Default.Email,
                            contentDescription = "Chat",
                            tint = iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        ContentScreen(modifier = Modifier.padding(innerPadding), selectedPage)
    }
}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex : Int) {
    when(selectedIndex){
        0 -> ChatsScreen()
        1 -> BookmarksScreen()
        2 -> PropertySearchScreen()
        3 -> ProfileEditorScreen(onProfileSaved = {})
        4 -> OptionsScreen()
    }

}
