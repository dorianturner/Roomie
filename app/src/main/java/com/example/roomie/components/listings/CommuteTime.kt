package com.example.roomie.components.listings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import com.example.roomie.BuildConfig

val client = OkHttpClient()
suspend fun getCommuteTime(from: String, to: String): Int? = withContext(Dispatchers.IO) {
    if (from.isBlank() || to.isBlank()) {
        return@withContext null
    }

    val appKey = BuildConfig.TFL_API_KEY

    val encodedFrom = URLEncoder.encode(from, "UTF-8").replace("+", "%20")
    val encodedTo = URLEncoder.encode(to, "UTF-8").replace("+", "%20")

    val url = "https://api.tfl.gov.uk/Journey/JourneyResults/$encodedFrom/to/$encodedTo?app_key=$appKey"

    val request = Request.Builder().url(url).build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("TFL API Error: ${response.code} - ${response.message}")
                println("Response body: ${response.body?.string()}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            // takes the first journey’s duration
            if (json.has("journeys") && json.getJSONArray("journeys").length() > 0) {
                val journey = json.getJSONArray("journeys").getJSONObject(0)
                return@withContext journey.getInt("duration")
            }
            return@withContext null
        }
    } catch (e: Exception) {
        println("TFL API Exception: ${e.message}")
        e.printStackTrace()
        return@withContext null
    }
}

suspend fun getMassCommuteTime(froms: List<String>, to: String): Int {
    var maxCommute = 0
    for (from in froms) {
        val cur = getCommuteTime(from, to)
        if (cur != null && cur > maxCommute) {
            maxCommute = cur
        }
    }
    return maxCommute
}
