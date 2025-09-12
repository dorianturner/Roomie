package com.example.roomie.components.chat

import android.content.Context
import android.util.Log
import com.example.roomie.components.cancelMerge
import com.example.roomie.components.cancelMergeTransaction
import com.example.roomie.components.finaliseMergeGroups
import kotlinx.coroutines.tasks.await
import kotlin.collections.containsAll

class PollManager(private val chatManager: ChatManager) {

    private val endEffectsRegistry: MutableMap<String, suspend (Poll) -> Unit> = mutableMapOf(
        "merge" to { poll ->
            if (poll.resolution == "Unanimous Yes") {
                Log.d("PollManager", "Unanimous Yes detected, merging conversations")
                val convoSnap = chatManager.convoRef.get().await()
                val participantUids =
                    convoSnap.get("participants") as? List<String> ?: emptyList()
                val groupIds = participantUids.mapNotNull { uid ->
                    val userSnap =
                        chatManager.db.collection("users").document(uid).get().await()
                    userSnap.getString("groupId")
                }.toSet()
                if (groupIds.size == 2) {
                    val success = finaliseMergeGroups(groupIds.first(), groupIds.last())
                    if (success) {
                        Log.d("PollManager", "Merged groups ${groupIds.first()} and ${groupIds.last()}")
                    } else {
                        Log.e("PollManager", "Failed to merge groups ${groupIds.first()} and ${groupIds.last()}")
                    }
                } else {
                    Log.d("PollManager", "Not enough groups to merge, expected 2, got ${groupIds.size}")
                }
                // TODO: delete subgroup convos

            } else {
                Log.d("PollManager", "Unanimous Yes not detected, not merging conversations")
                val convoSnap = chatManager.convoRef.get().await()
                val participantUids =
                    convoSnap.get("participants") as? List<String> ?: emptyList()
                val groupIds = participantUids.mapNotNull { uid ->
                    val userSnap =
                        chatManager.db.collection("users").document(uid).get().await()
                    userSnap.getString("groupId")
                }.toSet()
                if (groupIds.size == 2) {
                    cancelMerge(groupIds.first(), groupIds.last())
                }
                // TODO: delete convo
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

    private val voteEffectsRegistry: MutableMap<String, suspend (Context, String, Poll) -> Unit> = mutableMapOf(
        "merge" to { context, uid, poll ->
            if (poll.votes[uid] == "no") {
                val cancelled = try {
                    chatManager.db.runTransaction { transaction ->
                        Log.d("PollManager", "User $uid voted no, vetoing merge")
                        val convoSnap = transaction.get(chatManager.convoRef)
                        val participantUids =
                            convoSnap.get("participants") as? List<String> ?: emptyList()
                        val groupIds = participantUids.mapNotNull { uid ->
                            val userSnap =
                                transaction.get(chatManager.db.collection("users").document(uid))
                            userSnap.getString("groupId")
                        }.toSet()
                        if (groupIds.size == 2) {
                            cancelMergeTransaction(groupIds.first(), groupIds.last(), transaction)
                        }

                        transaction.update(chatManager.convoRef, "activePoll", null)
                    }.await()
                    true
                } catch (e: Throwable) {
                    Log.e("PollManager", "Error cancelling merge", e)
                    false
                }
                if (cancelled) {
                    chatManager.sendMessage(
                        context = context,
                        senderId = "system",
                        type = "system",
                        text = "Poll: ${poll.question}\nResolution: Vetoed"
                    )
                }
            }
        },
        "finalise" to { context, uid, poll ->
            if (poll.votes[uid] == "no") {
                Log.d("PollManager", "User $uid voted no, vetoing finalisation")

                chatManager.convoRef.update("activePoll", null).await()

                chatManager.sendMessage(
                    context = context,
                    senderId = "system",
                    type = "system",
                    text = "Poll: ${poll.question}\nResolution: Vetoed"
                )
            }
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
                endEffectsRegistry[poll.type]?.invoke(poll)
            } else {
                voteEffectsRegistry[poll.type]?.invoke(context, userId, poll)
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