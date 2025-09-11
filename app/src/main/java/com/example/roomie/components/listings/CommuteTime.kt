package com.example.roomie.components.listings

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import com.example.roomie.BuildConfig

val client = OkHttpClient()

fun getCommuteTime(from: String, to: String): Int? {
    if (from == "") {
        return null
    }

    val appKey = BuildConfig.TFL_API_KEY

    val encodedFrom = URLEncoder.encode(from, "UTF-8")
    val encodedTo = URLEncoder.encode(to, "UTF-8")

    val url = "https://api.tfl.gov.uk/Journey/JourneyResults/$encodedFrom/to/$encodedTo" +
            "?app_key=$appKey"

    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Unexpected code $response")

        val body = response.body?.string() ?: return null
        val json = JSONObject(body)

        // takes the first journey’s duration
        val journeys = json.getJSONArray("journeys")
        if (journeys.length() > 0) {
            return journeys.getJSONObject(0).getInt("duration")
        }
    }
    return null
}

fun getMassCommuteTime(froms: List<String>, to: String): Int {
    var maxCommute = 0
    for (from in froms) {
        val cur = getCommuteTime(from, to)
        if (cur != null && cur > maxCommute) {
            maxCommute = cur
        }
    }
    return maxCommute
}
