package com.example.roomie.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.roomie.screens.MainContentScreen
import com.example.roomie.screens.ProfileEditorScreen
import com.example.roomie.screens.SplashScreen
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

/**
 * Defines the navigation routes for your application.
 */
object Routes {
    const val SPLASH_SCREEN = "splash_screen"
    const val MAIN_CONTENT = "main_content"
    const val PROFILE_EDITOR = "profile_editor"
}

/**
 * The main navigation graph for your Roomie app.
 *
 * @param navController The NavHostController to manage navigation.
 * @param startDestination The initial route where the NavHost should start.
 *                         This will be determined by the authentication state in MainActivity.
 */
@Composable
fun RoomieNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SPLASH_SCREEN) {
            SplashScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN_CONTENT) {
                        popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                    }
                },
                onCreateAccountSuccess = {
                    navController.navigate(Routes.PROFILE_EDITOR) {
                        popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN_CONTENT) {
            MainContentScreen(
                onEditProfile = {
                    // Navigate to the profile editor screen when the button is clicked
                    navController.navigate(Routes.PROFILE_EDITOR)
                },
                onLogout = {
                    Firebase.auth.signOut()
                    navController.navigate(Routes.SPLASH_SCREEN) {
                        popUpTo("mainContent") { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PROFILE_EDITOR) {
            ProfileEditorScreen(
                onProfileSaved = {
                    navController.navigate(Routes.MAIN_CONTENT) {
                        popUpTo(Routes.MAIN_CONTENT) { inclusive = true } // Clear back stack up to main content
                    }
                }
            )
        }
    }
}
