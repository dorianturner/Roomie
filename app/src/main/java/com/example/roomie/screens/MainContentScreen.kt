package com.example.roomie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.roomie.components.LogoutAlertDialog
import com.example.roomie.components.NavigationBarItem
import com.example.roomie.components.RoomieNameLogo
import com.example.roomie.components.chat.ChatManager
import com.example.roomie.navigation.Routes
import com.example.roomie.ui.theme.Spacing
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContentScreen(
    onEditProfile: () -> Unit, // New callback for editing profile
    onNavigateToChat: ()-> Unit,
    onLogout: () -> Unit,
) {

    val childNavController = rememberNavController()
    var showMenu by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    val navBackStackEntry by childNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    val navBarItemList = listOf(
        NavigationBarItem("Chats", Icons.AutoMirrored.Filled.Chat),
        NavigationBarItem("Bookmarks", Icons.Default.Favorite),
        NavigationBarItem("Discover", Icons.Default.People),
        NavigationBarItem("Search", Icons.Default.Search),
        NavigationBarItem("Profile", Icons.Default.Person),
    )

    val navigationMap: Map<Int, () -> Unit> = mapOf(
        0 to { childNavController.navigate("chats") },
        1 to { childNavController.navigate("bookmarks") },
        2 to { childNavController.navigate("discover") },
        3 to { childNavController.navigate("search") },
        4 to { childNavController.navigate("profile") },
    )

    // To indicate which icon to highlight at the navBar
    val selectedPage = when (currentRoute) {
        "chats" -> 0
        "bookmarks" -> 1
        "discover" -> 2
        "search" -> 3
        "single_listing/{listingId}" -> 3
        "profile" -> 4
        "profile_editor" -> 4
        Routes.ADD_LISTING -> 4
        "edit_listing/{listingId}" -> 4
        else -> 0
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
                                navigationMap[index]?.invoke() ?: run {
                                    childNavController.navigate("chats")
                                }
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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            NavHost(
                navController = childNavController,
                startDestination = "chats",
                modifier = Modifier
            ) {
                composable("chats") {
                    ChatsScreen(
                        navController = childNavController
                    )
                }
                composable("bookmarks") {
                    BookmarksScreen()
                }
                composable("discover") {
                    UserDiscoveryScreen(childNavController)
                }
                composable("search") {
                    PropertySearchScreen(navController = childNavController)
                }
                composable("profile") {
                    ProfileScreen(navController = childNavController)
                }
                composable("options") {
                    OptionsScreen()
                }
                composable("profile_editor") {
                    ProfileEditorScreen(onProfileSaved = {})
                }
                composable(Routes.ADD_LISTING) {
                    EditListingScreen(navController = childNavController)
                }
                composable(
                    "chat/{chatId}/{chatName}",
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("chatName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId")!!
                    val chatName = backStackEntry.arguments?.getString("chatName")!!
                    SingleChatScreen(
                        chatManager = ChatManager(chatId),
                        chatName = chatName,
                        onBack = { childNavController.popBackStack() }
                    )
                }
                composable(
                    "single_listing/{listingId}",
                    arguments = listOf(
                        navArgument("listingId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                    SingleListingScreen(
                        listingId = listingId,
                        onBack = { childNavController.popBackStack() },
                        navController = childNavController
                    )
                }
                composable(
                    "edit_listing/{listingId}",
                    arguments = listOf(
                        navArgument("listingId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                    EditListingScreen(
                        navController = childNavController,
                        listingId = listingId
                    )
                }
            }
        }
    }
}
