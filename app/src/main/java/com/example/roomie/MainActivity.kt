package com.example.roomie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.roomie.navigation.RoomieNavHost
import com.example.roomie.navigation.Routes
import com.example.roomie.ui.theme.RoomieTheme // Your app's theme
import com.google.firebase.auth.auth

import com.google.firebase.firestore.ktx.firestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = Firebase.auth

        setContent {
            RoomieTheme {
                Surface {
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val user = auth.currentUser
                        if (user == null) { // if user is not already logged in
                            startDestination = Routes.SPLASH_SCREEN // go to login screen
                        } else { // if user is logged in
                            // check if user's profile is completed
                            Firebase.firestore.collection("users").document(user.uid).get()
                                .addOnSuccessListener { doc ->
                                    startDestination = if (doc.getBoolean("minimumRequiredProfileSet") == true) {
                                        Routes.MAIN_CONTENT // take to main page if it is
                                    } else {
                                        Routes.ONBOARDING_FLOW // otherwise go finish setup
                                    }
                                }
                                .addOnFailureListener {
                                    startDestination = Routes.SPLASH_SCREEN // fallback
                                }
                        }
                    }

                    if (startDestination != null) { // should never be null
                        RoomieNavHost(
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }

    // Optional: Lifecycle methods to manage authentication state
    // You can add onStart() if you want to re-check user status every time the activity starts,
    // which might be useful if the app was in the background for a long time.
    // For many apps, checking once in onCreate is sufficient for initial routing.

    /*
    public override fun onStart() {
        super.onStart()
        // Here you could re-evaluate auth.currentUser if you need dynamic redirection
        // based on external auth changes (e.g., user signed out from another device).
        // However, for typical app flow, the onCreate check and NavHost handles it well.
    }
    */
}
