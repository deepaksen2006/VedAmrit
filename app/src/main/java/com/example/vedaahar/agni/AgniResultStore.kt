package com.example.vedaahar.agni

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AgniResultStore {
    private const val PREF_NAME = "vedaahar_agni_result"
    private const val KEY_RESULT = "latest_result"

    fun save(context: Context, result: AgniResult) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RESULT, result.toJson().toString())
            .apply()
    }

    fun current(context: Context): AgniResult? {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_RESULT, null)
            ?: return null
        return runCatching { raw.toAgniResult() }.getOrNull()
    }
}

private fun AgniResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("answers", answers.mapKeys { it.key.toString() }.toJsonObject())
        put("scores", scores.mapKeys { it.key.name }.toJsonObject())
        put("dominantAgni", dominantAgni)
        put("mixedAgniTypes", JSONArray(mixedAgniTypes))
        put("isMixed", isMixed)
        put("completedAtMillis", completedAtMillis)
    }
}

private fun String.toAgniResult(): AgniResult {
    val json = JSONObject(this)
    return AgniResult(
        answers = json.getJSONObject("answers").toIntStringMap(),
        scores = json.getJSONObject("scores").toAgniIntMap(),
        dominantAgni = json.getString("dominantAgni"),
        mixedAgniTypes = json.getJSONArray("mixedAgniTypes").toStringList(),
        isMixed = json.getBoolean("isMixed"),
        completedAtMillis = json.getLong("completedAtMillis")
    )
}

private fun Map<String, Any>.toJsonObject(): JSONObject {
    return JSONObject().also { json ->
        forEach { (key, value) -> json.put(key, value) }
    }
}

private fun JSONObject.toIntStringMap(): Map<Int, String> {
    return keys().asSequence().associate { key -> key.toInt() to getString(key) }
}

private fun JSONObject.toAgniIntMap(): Map<AgniType, Int> {
    return keys().asSequence().associate { key -> AgniType.valueOf(key) to getInt(key) }
}

private fun JSONArray.toStringList(): List<String> {
    return (0 until length()).map { index -> getString(index) }
}
