package com.example.roomie.components

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.functions

object FunctionsProvider {
    val instance = Firebase.functions("us-central1")

    init {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("FunctionsProvider", "User is not authenticated")
        }
    }
}
