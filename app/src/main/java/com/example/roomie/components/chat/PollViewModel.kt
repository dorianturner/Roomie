package com.example.roomie.components.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PollViewModel (
    private val pollManager: PollManager
) : ViewModel() {
    fun castVote(userId: String, choice: String, context: Context) {
        viewModelScope.launch {
            try {
                pollManager.castVote(context, userId, choice)
            } catch (e: Exception) {
                Log.e("PollViewModel", "Failed to cast vote: ${e.message}")
            }
        }
    }
}