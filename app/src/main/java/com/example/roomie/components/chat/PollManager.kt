package com.example.roomie.components.chat

import android.content.Context
import android.util.Log
import com.example.roomie.components.cancelMerge
import com.example.roomie.components.finaliseMergeGroups
import com.example.roomie.components.mergeGroups
import kotlinx.coroutines.tasks.await
import kotlin.collections.containsAll

class PollManager(private val chatManager: ChatManager) {

    private val endEffectsRegistry: MutableMap<String, suspend (Poll) -> Unit> = mutableMapOf(
        "merge" to { poll ->
            if (poll.resolution == "Unanimous Yes") {
                Log.d("PollManager", "Unanimous Yes detected, merging groups")

                val convoSnap = chatManager.convoRef.get().await()
                val participantUids = convoSnap.get("participants") as? List<String> ?: emptyList()

                val groupIds = participantUids.mapNotNull { uid ->
                    val userSnap = chatManager.db.collection("users").document(uid).get().await()
                    userSnap.getString("groupId")
                }.toSet()

                if (groupIds.size == 2) {
                    val groupA = groupIds.elementAt(0)
                    val groupB = groupIds.elementAt(1)

                    // Stage 1: Finalise merging group
                    val mergeFinalised = finaliseMergeGroups(groupA, groupB)
                    if (!mergeFinalised) {
                        Log.e("PollManager", "Failed to merge $groupA and $groupB")
                        return@to
                    }

                    try {
                        chatManager.convoRef.update(
                            mapOf(
                                "chatType" to ChatType.MY_GROUP.name,
                                "isGroup" to true,
                                "activePoll" to null
                            )
                        ).await()
                        Log.d("PollManager", "Conversation updated to MY_GROUP after merge")
                    } catch (e: Exception) {
                        Log.e("PollManager", "Error updating conversation after server finalise: ${e.message}", e)
                    }
                } else {
                    Log.d("PollManager", "Not enough groups to merge, expected 2, got ${groupIds.size}")
                }

            } else {
                Log.d("PollManager", "Poll failed, not merging groups")

                val convoSnap = chatManager.convoRef.get().await()
                val participantUids = convoSnap.get("participants") as? List<String> ?: emptyList()

                val groupIds = participantUids.mapNotNull { uid ->
                    val userSnap = chatManager.db.collection("users").document(uid).get().await()
                    userSnap.getString("groupId")
                }.toSet()

                if (groupIds.size == 2) {
                    cancelMerge(groupIds.first(), groupIds.last())
                }

                chatManager.convoRef.delete().await()
                Log.d("PollManager", "Conversation ${chatManager.convoRef.id} deleted after failed merge")
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
                try {
                    Log.d("PollManager", "User $uid voted no, vetoing merge")
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

                    Log.d("PollManager", "Conversation ${chatManager.convoRef.id} deleted after failed merge")
                    true
                } catch (e: Throwable) {
                    Log.e("PollManager", "Error cancelling merge", e)
                    false
                } finally {
                    chatManager.deleteConversation()
                }
            } else if (poll.votes[uid] == "yes") {
                // if anyone votes yes, try to initiate merge
                try {
                    Log.d("PollManager", "User $uid voted yes, attempting to initialise merge")
                    val convoSnap = chatManager.convoRef.get().await()
                    val participantUids =
                        convoSnap.get("participants") as? List<String> ?: emptyList()
                    val groupIds = participantUids.mapNotNull { uid ->
                        val userSnap =
                            chatManager.db.collection("users").document(uid).get().await()
                        userSnap.getString("groupId")
                    }.toSet()
                    if (groupIds.size == 2) {
                        mergeGroups(groupIds.first(), groupIds.last())
                    }

                    Log.d("PollManager","Successfully initiated merge")
                    true
                } catch (e: Throwable) {
                    Log.e("PollManager", "Error initiating merge", e)
                    false
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

    suspend fun createPoll(question: String, pollType: String) : Boolean {
        return try {
            chatManager.db.runTransaction { transaction ->
                val convoSnap = transaction.get(chatManager.convoRef)
                val existing = convoSnap.get("activePoll")
                if (existing != null) {
                    false
                } else {
                    val poll = Poll(question = question, type = pollType)
                    val pollMap = mapOf(
                        "question" to poll.question,
                        "votes" to poll.votes,
                        "closed" to poll.closed,
                        "resolution" to poll.resolution,
                        "type" to poll.type
                    )
                    transaction.update(chatManager.convoRef, "activePoll", pollMap)
                    true
                }
            }.await()
        } catch (e: Exception) {
            Log.e("PollManager", "createPollIfMissing error: ${e.message}", e)
            false
        }
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