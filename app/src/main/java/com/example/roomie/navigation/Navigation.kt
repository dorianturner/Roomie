package com.example.roomie.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.roomie.screens.MainContentScreen
import com.example.roomie.screens.ProfileCreationScreen
import com.example.roomie.screens.SplashScreen

/**
 * Defines the navigation routes for your application.
 */
object Routes {
    const val SPLASH_SCREEN = "splash_screen"
    const val MAIN_CONTENT = "main_content"
    const val PROFILE_CREATION = "profile_creation"
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
                    // Navigate to main content and remove splash screen from back stack
                    navController.navigate(Routes.MAIN_CONTENT) {
                        popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                    }
                },
                onCreateAccountSuccess = {
                    // Navigate to profile creation and remove splash screen from back stack
                    navController.navigate(Routes.PROFILE_CREATION) {
                        popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN_CONTENT) {
            MainContentScreen()
            // Here you might pass a ViewModel or other dependencies to MainContentScreen
        }
        composable(Routes.PROFILE_CREATION) {
            ProfileCreationScreen()
            // Here you might pass a ViewModel or other dependencies to ProfileCreationScreen
        }
    }
}
