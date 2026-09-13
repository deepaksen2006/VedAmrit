package com.example.vedaahar.symptoms

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SymptomsAnalysisStore {
    private const val PREF_NAME = "vedaahar_symptoms_analysis"
    private const val KEY_RESULT = "latest_result"

    fun save(context: Context, result: SymptomsAnalysisResult) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RESULT, result.toJson().toString())
            .apply()
    }

    fun current(context: Context): SymptomsAnalysisResult? {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_RESULT, null)
            ?: return null
        return runCatching { raw.toSymptomsAnalysisResult() }.getOrNull()
    }
}

private fun SymptomsAnalysisResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("symptoms", symptoms.toJsonObject())
        put("reportedSymptoms", JSONArray(reportedSymptoms.map { it.key }))
        put("possibleCondition", possibleCondition)
        put("completedAtMillis", completedAtMillis)
    }
}

private fun String.toSymptomsAnalysisResult(): SymptomsAnalysisResult {
    val json = JSONObject(this)
    val symptoms = json.getJSONObject("symptoms").toIntMap()
    val reportedKeys = json.getJSONArray("reportedSymptoms").toStringList().toSet()
    return SymptomsAnalysisResult(
        symptoms = symptoms,
        reportedSymptoms = symptomFeatures.filter { it.key in reportedKeys },
        possibleCondition = json.optString("possibleCondition").takeIf { it.isNotBlank() && it != "null" },
        completedAtMillis = json.getLong("completedAtMillis")
    )
}

private fun Map<String, Int>.toJsonObject(): JSONObject {
    return JSONObject().also { json ->
        forEach { (key, value) -> json.put(key, value) }
    }
}

private fun JSONObject.toIntMap(): Map<String, Int> {
    return keys().asSequence().associateWith { key -> getInt(key) }
}

private fun JSONArray.toStringList(): List<String> {
    return (0 until length()).map { index -> getString(index) }
}
