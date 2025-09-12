package com.example.roomie.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roomie.components.OnboardingProfileState
import com.example.roomie.components.soundManager.LocalSoundManager
import com.example.roomie.components.soundManager.rememberSoundManager
import com.example.roomie.screens.BasicInfoScreen
import com.example.roomie.screens.EditListingScreen
import com.example.roomie.screens.ExtraInfoScreen
import com.example.roomie.screens.MainContentScreen
import com.example.roomie.screens.ProfileEditorScreen
import com.example.roomie.screens.ProfileTypeScreen
import com.example.roomie.screens.SplashScreen
import com.example.roomie.screens.TellMoreScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Defines the navigation routes for your application.
 */
object Routes {
    const val SPLASH_SCREEN = "splash_screen" // firebase auth
    const val MAIN_CONTENT = "main_content"
    const val PROFILE_EDITOR = "profile_editor"
    const val CHAT_SCREEN = "chat_screen"

    // onboarding pathway
    const val ONBOARDING_FLOW = "onboarding_flow" // general onboarding subgraph
    const val BASIC_INFO = "basic_info" // name, age, phone number
    const val PROFILE_TYPE = "profile_type" // student or landlord
    const val EXTRA_INFO = "extra_info" // specific student/landlord info

    const val TELL_MORE = "tell_more"

    const val ADD_LISTING = "add_listing"
}

/**
 * The main navigation graph for your Roomie app.
 *
 * @param startDestination The initial route where the NavHost should start.
 *                         This will be determined by the authentication state in MainActivity.
 */
@Composable
fun RoomieNavHost(
    startDestination: String
) {
    val navController = rememberNavController()
    val soundManager = rememberSoundManager()
    CompositionLocalProvider(LocalSoundManager provides soundManager) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.SPLASH_SCREEN) {
                SplashScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.MAIN_CONTENT) {
                            popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                        }
                    },
                    onCreateAccountSuccess = {
                        navController.navigate(Routes.ONBOARDING_FLOW) {
                            popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING_FLOW) {
                OnboardingFlow(
                    onFinish = {
                        navController.navigate(Routes.MAIN_CONTENT) {
                            popUpTo(Routes.ONBOARDING_FLOW) { inclusive = true }
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
                    onNavigateToChat = {
                        navController.navigate(Routes.CHAT_SCREEN) {
                            popUpTo("chatScreen") { inclusive = true }
                        }
                    },
                    onLogout = {
                        Firebase.auth.signOut()
                        navController.navigate(Routes.SPLASH_SCREEN) {
                            popUpTo("mainContent") { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.PROFILE_EDITOR) {
                ProfileEditorScreen(
                    onProfileSaved = {
                        navController.navigate(Routes.MAIN_CONTENT) {
                            popUpTo(Routes.MAIN_CONTENT) {
                                inclusive = true
                            } // Clear back stack up to main content
                        }
                    }
                )
            }
        }

        composable(Routes.ADD_LISTING) { EditListingScreen(navController = navController) }
    }
}

@Composable
fun OnboardingFlow(
    onFinish: () -> Unit,
) {
    val profileState = remember { mutableStateOf(OnboardingProfileState()) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.BASIC_INFO) {
        composable(Routes.BASIC_INFO) {
            BasicInfoScreen(
                profileState = profileState.value,
                onNext = { navController.navigate(Routes.PROFILE_TYPE) }
            )
        }
        composable(Routes.PROFILE_TYPE) {
            ProfileTypeScreen(
                profileState = profileState.value,
                onProfileStateChange = { newState: OnboardingProfileState -> profileState.value = newState },
                onNext = { navController.navigate(Routes.EXTRA_INFO) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EXTRA_INFO) {
            // IMPORTANT: pass an onFinish that routes to TELL_MORE for students,
            // otherwise calls the final onFinish (which goes to main content).
            ExtraInfoScreen(
                profileState = profileState.value,
                onFinish = {
                    // if student, continue to TellMore; if landlord, finish onboarding
                    if (!profileState.value.isLandlord) {
                        navController.navigate(Routes.TELL_MORE)
                    } else {
                        onFinish()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TELL_MORE) {
            TellMoreScreen(
                profileState = profileState.value,
                onNext = {
                    // finished onboarding after TellMore
                    onFinish()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
