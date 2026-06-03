package com.example.vedaahar.dosha

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object DoshaResultStore {
    private const val PreferencesName = "dosha_result_history"
    private const val HistoryKey = "assessments"

    fun current(context: Context): SavedDoshaAssessment? {
        return history(context).lastOrNull()
    }

    fun historyCount(context: Context): Int {
        return history(context).size
    }

    fun history(context: Context): List<SavedDoshaAssessment> {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val rawHistory = preferences.getString(HistoryKey, "[]").orEmpty()
        val jsonArray = runCatching { JSONArray(rawHistory) }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                add(item.toSavedAssessment())
            }
        }
    }

    fun save(context: Context, result: DoshaResult, savedAtMillis: Long = System.currentTimeMillis()): SavedDoshaAssessment? {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val existing = history(context)
        val previous = existing.lastOrNull()
        val updatedHistory = JSONArray()

        existing.forEach { savedAssessment ->
            updatedHistory.put(savedAssessment.toJson())
        }
        updatedHistory.put(result.toSavedAssessment(savedAtMillis).toJson())

        preferences.edit()
            .putString(HistoryKey, updatedHistory.toString())
            .apply()

        return previous
    }

    private fun DoshaResult.toSavedAssessment(savedAtMillis: Long): SavedDoshaAssessment {
        return SavedDoshaAssessment(
            profileName = profileName,
            dominantName = dominant.displayName,
            overview = description.overview,
            percentages = percentages,
            savedAtMillis = savedAtMillis
        )
    }

    private fun SavedDoshaAssessment.toJson(): JSONObject {
        return JSONObject()
            .put("profileName", profileName)
            .put("dominantName", dominantName)
            .put("overview", overview)
            .put("savedAtMillis", savedAtMillis)
            .put("vataPercent", percentages[Dosha.Vata] ?: 0)
            .put("pittaPercent", percentages[Dosha.Pitta] ?: 0)
            .put("kaphaPercent", percentages[Dosha.Kapha] ?: 0)
    }

    private fun JSONObject.toSavedAssessment(): SavedDoshaAssessment {
        return SavedDoshaAssessment(
            profileName = optString("profileName", "Kapha"),
            dominantName = optString("dominantName", optString("profileName", "Kapha")),
            overview = optString("overview", "Lightening foods, regular movement, and stimulation throughout the day help prevent sluggishness."),
            percentages = mapOf(
                Dosha.Vata to optInt("vataPercent", 0),
                Dosha.Pitta to optInt("pittaPercent", 0),
                Dosha.Kapha to optInt("kaphaPercent", 0)
            ),
            savedAtMillis = optLong("savedAtMillis", 0L)
        )
    }
}
