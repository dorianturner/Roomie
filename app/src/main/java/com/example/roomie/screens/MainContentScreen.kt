package com.example.roomie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.components.NavigationBarItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContentScreen(
    onEditProfile: () -> Unit, // New callback for editing profile
    onLogout: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val iconColor = MaterialTheme.colorScheme.primary

    val navBarItemList = listOf(
        NavigationBarItem("Chats", Icons.AutoMirrored.Filled.Chat),
        NavigationBarItem("Bookmarks", Icons.Default.Favorite),
        NavigationBarItem("Search", Icons.Default.Search),
        NavigationBarItem("Profile", Icons.Default.Person),
        NavigationBarItem("Options", Icons.Default.MoreVert)
    )

    var selectedPage by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
                            .padding(horizontal = 48.dp, vertical = 12.dp), // Prevents overlap

                        // Can put in below to give the header it's own colour
                        // color = MaterialTheme.colorScheme.primaryContainer,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
