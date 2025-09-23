package com.example.roomie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.example.roomie.navigation.RoomieNavHost
import com.example.roomie.navigation.Routes
import com.example.roomie.ui.theme.RoomieTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        // enable caching
        val db = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        db.firestoreSettings = settings

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
}
