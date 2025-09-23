package com.example.roomie.components.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel for managing poll-related operations.
 *
 * @property pollManager The manager responsible for handling poll logic.
 */
class PollViewModel (
    private val pollManager: PollManager
) : ViewModel() {
    /**
     * Casts a vote in a poll.
     *
     * This function launches a coroutine in the viewModelScope to asynchronously cast a vote.
     * Any exceptions during the voting process are caught and logged.
     *
     * @param userId The ID of the user casting the vote.
     * @param choice The choice selected by the user.
     * @param context The Android context, used by the [PollManager].
     */
    fun castVote(userId: String, choice: String, context: Context) {
        viewModelScope.launch {
            try {
                pollManager.castVote(context, userId, choice)
            } catch (e: Exception) {
                Log.e("PollViewModel", "Failed to cast vote.", e)
            }
        }
    }
}

