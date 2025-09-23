package com.example.roomie.components

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.functions

/**
 * Provides a singleton instance of Firebase Functions.
 * This object is responsible for initializing and configuring the Firebase Functions client.
 */
object FunctionsProvider {
    /**
     * The Firebase Functions instance, configured for the "us-central1" region.
     * This instance can be used to call Cloud Functions.
     */
    val instance = Firebase.functions("us-central1")

    /**
     * Initializes the FunctionsProvider.
     * Checks if a user is currently authenticated with Firebase Auth.
     * Logs an error if no user is authenticated, as Firebase Functions calls might require authentication.
     */
    init {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("FunctionsProvider", "User is not authenticated")
        }
    }
}
