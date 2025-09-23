package com.example.roomie.components

import com.google.firebase.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Formats a Firebase [Timestamp] into a human-readable string representation.
 *
 * The formatting rules are as follows:
 * - If the timestamp is from the same day, it returns the time (e.g., "HH:mm").
 * - If the timestamp is from yesterday, it returns "Yesterday HH:mm".
 * - If the timestamp is from the current week (but not today or yesterday), it returns the day of the week and time (e.g., "Mon HH:mm").
 * - If the timestamp is from the current year (but not the current week), it returns the short date and time (e.g., "M/d HH:mm").
 * - Otherwise (for previous years), it returns the short date (including year) and time (e.g., "M/d/yy HH:mm").
 *
 * @param timestamp The Firebase [Timestamp] to format.
 * @return A formatted string representing the timestamp.
 */
fun formatTimestamp(timestamp: Timestamp): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply {
        time = timestamp.toDate()
    }

    val sameDay = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
            && now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)

    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val isYesterday = msgTime.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            msgTime.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timePart = timeFormat.format(msgTime.time)

    return when {
        sameDay -> timePart
        isYesterday -> "Yesterday $timePart"
        now.get(Calendar.WEEK_OF_YEAR) == msgTime.get(Calendar.WEEK_OF_YEAR) &&
                now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) -> {
            val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(msgTime.time)
            "$weekday $timePart"
        }
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) -> {
            val date = DateFormat.getDateInstance(DateFormat.SHORT).format(msgTime.time)
            "$date $timePart"
        }
        else -> {
            val df = DateFormat.getDateInstance(DateFormat.SHORT) as SimpleDateFormat
            val pattern = df.toPattern()
            val newPattern = if (!pattern.contains("y")) "$pattern/yy" else pattern
            val fullDate = SimpleDateFormat(newPattern, Locale.getDefault()).format(msgTime.time)
            "$fullDate $timePart"
        }
    }
}

