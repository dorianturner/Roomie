package com.example.roomie.components

import com.google.firebase.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

