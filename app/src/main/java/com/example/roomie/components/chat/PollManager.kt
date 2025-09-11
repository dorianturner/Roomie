package com.example.roomie.components.chat

import android.content.Context
import android.util.Log
import kotlinx.coroutines.tasks.await
import kotlin.collections.containsAll

class PollManager(private val chatManager: ChatManager) {

    private val effectsRegistry: MutableMap<String, suspend (Poll) -> Unit> = mutableMapOf(
        "merge" to { poll ->
            if (poll.resolution == "Unanimous Yes") {
                Log.d("PollManager", "Unanimous Yes detected, merging conversations")
            } else {
                Log.d("PollManager", "Unanimous Yes not detected, not merging conversations")
            }
        },
        "finalise" to { poll ->
            if (poll.resolution == "Unanimous Yes") {
                Log.d("PollManager", "Unanimous Yes detected, finalising group")
            } else {
                Log.d("PollManager", "Unanimous Yes not detected, not finalising group")
            }
        },
        "generic" to { poll ->
            Log.d("PollManager", "Generic poll effect triggered with result ${poll.resolution}")
        }
    )

    suspend fun createPoll(question: String, pollType: String) {
        val poll = Poll(question = question, type = pollType)
        chatManager.convoRef.update("activePoll", poll).await()
    }

    suspend fun castVote(context: Context, userId: String, choice: String) {
        val pollResult: Poll? = chatManager.db.runTransaction { transaction ->
            val snap = transaction.get(chatManager.convoRef)
            val poll = snap.toObject(Conversation::class.java)?.activePoll
                ?: throw IllegalStateException("No active poll")

            if (poll.closed) return@runTransaction null

            val updatedVotes = poll.votes.toMutableMap()
            updatedVotes[userId] = choice

            var isClosed = poll.closed
            var resolution = poll.resolution

            val participants = snap.get("participants") as? List<*> ?: emptyList<String>()

            if (updatedVotes.keys.containsAll(participants) && !updatedVotes.values.contains("undecided")) {
                resolution = calculateResolution(updatedVotes)
                isClosed = true

                transaction.update(chatManager.convoRef, "activePoll", null)
            } else {
                transaction.update(chatManager.convoRef, "activePoll", mapOf(
                    "question" to poll.question,
                    "votes" to updatedVotes.toMap(),
                    "closed" to false,
                    "resolution" to resolution,
                    "type" to poll.type,
                ))
            }

            poll.copy(votes = updatedVotes.toMap(), closed = isClosed, resolution = resolution, type = poll.type)
        }.await()

        pollResult?.let { poll ->
            if (poll.closed) {
                chatManager.sendMessage(
                    context = context,
                    senderId = "system",
                    type = "system",
                    text = "Poll: ${poll.question}\nResolution: ${poll.resolution}"
                )
                effectsRegistry[poll.type]?.invoke(poll)
            }
        }
    }

    private fun calculateResolution(votes: Map<String, String>): String {
        val yesCount = votes.values.count { it == "yes" }
        val noCount = votes.values.count { it == "no" }
        return when {
            yesCount > noCount && noCount == 0 -> "Unanimous Yes"
            yesCount > noCount -> "Majority Yes"
            noCount > yesCount && yesCount == 0 -> "Unanimous No"
            noCount > yesCount -> "Majority No"
            else -> "Draw"
        }
    }
}