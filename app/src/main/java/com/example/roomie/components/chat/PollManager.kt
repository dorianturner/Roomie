package com.example.roomie.components.chat

import android.content.Context
import android.util.Log
import com.example.roomie.components.cancelMerge
import com.example.roomie.components.finaliseGroup
import com.example.roomie.components.finaliseMergeGroups
import com.example.roomie.components.mergeGroups
import kotlinx.coroutines.tasks.await

/**
 * Manages polls within a chat conversation.
 *
 * This class handles the creation, voting, and resolution of polls,
 * as well as triggering side effects based on poll outcomes.
 *
 * @property chatManager The [ChatManager] instance for interacting with the current conversation.
 */
class PollManager(private val chatManager: ChatManager) {

    private val endEffectsRegistry: MutableMap<String, suspend (Poll) -> Unit> = mutableMapOf(
        "merge" to { poll ->
            if (poll.resolution == "Unanimous Yes") {
                Log.d("PollManager", "Unanimous Yes detected, merging groups")

                val convoSnap = chatManager.convoRef.get().await()
                val participantUids =
                    (convoSnap.get("participants") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()

                val groupIds = participantUids.mapNotNull { uid ->
                    val userSnap =
                        chatManager.db
                            .collection("users")
                            .document(uid)
                            .get()
                            .await()
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
                        Log.d(
                            "PollManager",
                            "Conversation updated to MY_GROUP after merge"
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "PollManager",
                            "Error updating conversation after server finalise: ${e.message}",
                            e
                        )
                    }
                } else {
                    Log.d(
                        "PollManager",
                        "Not enough groups to merge, expected 2, got ${groupIds.size}"
                    )
                }

            } else {
                Log.d("PollManager", "Poll failed, not merging groups")

                val convoSnap = chatManager.convoRef.get().await()
                val participantUids =
                    (convoSnap.get("participants") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()

                val groupIds = participantUids.mapNotNull { uid ->
                    val userSnap =
                        chatManager.db
                            .collection("users")
                            .document(uid)
                            .get()
                            .await()
                    userSnap.getString("groupId")
                }.toSet()

                if (groupIds.size == 2) {
                    cancelMerge(groupIds.first(), groupIds.last())
                }

                chatManager.convoRef.delete().await()
                Log.d(
                    "PollManager",
                    "Conversation ${chatManager.convoRef.id} deleted after failed merge"
                )
            }
        },
        "finalise" to { poll ->
            if (poll.resolution == "Unanimous Yes") {
                Log.d("PollManager", "Unanimous Yes detected, finalising group")

                val convoSnap = chatManager.convoRef.get().await()
                val participantUids =
                    (convoSnap.get("participants") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()
                val groupId = participantUids.firstOrNull()?.let { uid ->
                    val userSnap =
                        chatManager.db
                            .collection("users")
                            .document(uid)
                            .get()
                            .await()
                    userSnap.getString("groupId")
                } ?: run {
                    Log.e("PollManager", "No participants found in conversation")
                    return@to
                }

                val groupFinalised = finaliseGroup(groupId)
                if (!groupFinalised) {
                    Log.e("PollManager", "Failed to finalise group $groupId")
                    return@to
                }
                Log.d("PollManager", "Group $groupId finalised")
            } else {
                Log.d(
                    "PollManager",
                    "Unanimous Yes not detected, not finalising group")
            }
        },
        "generic" to { poll ->
            Log.d(
                "PollManager",
                "Generic poll effect triggered with result ${poll.resolution}"
            )
        }
    )

    private val voteEffectsRegistry: MutableMap<String, suspend (Context, String, Poll) -> Unit>
    = mutableMapOf(
        "merge" to { context, uid, poll ->
            if (poll.votes[uid] == "no") {
                try {
                    Log.d("PollManager", "User $uid voted no, vetoing merge")
                    val convoSnap = chatManager.convoRef.get().await()
                    val participantUids =
                        (convoSnap.get("participants") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()
                    val groupIds = participantUids.mapNotNull { uid ->
                        val userSnap =
                            chatManager.db
                                .collection("users")
                                .document(uid)
                                .get()
                                .await()
                        userSnap.getString("groupId")
                    }.toSet()
                    if (groupIds.size == 2) {
                        cancelMerge(groupIds.first(), groupIds.last())
                    }

                    Log.d(
                        "PollManager",
                        "Conversation ${chatManager.convoRef.id} deleted after failed merge"
                    )
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
                    Log.d(
                        "PollManager",
                        "User $uid voted yes, attempting to initialise merge"
                    )
                    val convoSnap = chatManager.convoRef.get().await()
                    val participantUids =
                        (convoSnap.get("participants") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()
                    val groupIds = participantUids.mapNotNull { uid ->
                        val userSnap =
                            chatManager.db
                                .collection("users")
                                .document(uid)
                                .get()
                                .await()
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
                    senderId = "system",
                    type = "system",
                    text = "Poll: ${poll.question}\nResolution: Vetoed"
                )
            }
        }
    )

    /**
     * Creates a new poll in the current conversation.
     *
     * A new poll will only be created if there isn't an existing active poll.
     *
     * @param question The question for the poll.
     * @param pollType The type of the poll (e.g., "merge", "finalise", "generic").
     *                 This determines the side effects triggered by the poll's outcome.
     * @return `true` if the poll was created successfully, `false` otherwise (e.g., if a poll is already active).
     */
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
                    transaction.update(
                        chatManager.convoRef,
                        "activePoll",
                        pollMap
                    )
                    true
                }
            }.await()
        } catch (e: Exception) {
            Log.e("PollManager", "Error creating poll.", e)
            false
        }
    }

    /**
     * Casts a vote in the active poll for the current conversation.
     *
     * If the vote leads to the poll's closure (e.g., all participants have voted),
     * the poll's resolution is calculated, a system message is sent, and any
     * registered end effects for the poll type are triggered.
     *
     * If the poll remains open after the vote, any registered vote effects
     * for the poll type are triggered.
     *
     * @param context The Android [Context].
     * @param userId The ID of the user casting the vote.
     * @param choice The user's choice (e.g., "yes", "no").
     */
    suspend fun castVote(context: Context, userId: String, choice: String) {
        val pollResult: Poll? = chatManager.db.runTransaction { transaction ->
            val snap = transaction.get(chatManager.convoRef)
            val poll = snap.toObject(Conversation::class.java)?.activePoll

            check(poll != null) { "No active poll" }

            if (poll.closed) return@runTransaction null

            val updatedVotes = poll.votes.toMutableMap()
            updatedVotes[userId] = choice

            var isClosed = false
            var resolution = poll.resolution

            val participants = snap.get("participants") as? List<*> ?: emptyList<String>()

            if (updatedVotes.keys.containsAll(participants) &&
                !updatedVotes.values.contains("undecided")) {
                resolution = calculateResolution(updatedVotes)
                isClosed = true

                transaction.update(
                    chatManager.convoRef,
                    "activePoll",
                    null
                )
            } else {
                transaction.update(
                    chatManager.convoRef,
                    "activePoll",
                    mapOf(
                        "question" to poll.question,
                        "votes" to updatedVotes.toMap(),
                        "closed" to false,
                        "resolution" to resolution,
                        "type" to poll.type,
                    )
                )
            }

            poll.copy(
                votes = updatedVotes.toMap(),
                closed = isClosed,
                resolution = resolution,
                type = poll.type
            )
        }.await()

        pollResult?.let { poll ->
            if (poll.closed) {
                chatManager.sendMessage(
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

    /**
     * Calculates the resolution of a poll based on the collected votes.
     *
     * @param votes A map where keys are user IDs and values are their respective votes.
     * @return A string representing the poll's resolution (e.g., "Unanimous Yes", "Majority No", "Draw").
     */
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
