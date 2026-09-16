package com.example.vedaahar.prakriti

import android.content.Context
import org.json.JSONObject

object PrakritiResultStore {
    private const val PREF_NAME = "vedaahar_prakriti_result"
    private const val KEY_RESULT = "latest_result"

    fun save(context: Context, result: PrakritiResult) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RESULT, result.toJson().toString())
            .apply()
    }

    fun current(context: Context): PrakritiResult? {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_RESULT, null)
            ?: return null
        return runCatching { raw.toPrakritiResult() }.getOrNull()
    }
}

private fun PrakritiResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("scores", scores.mapKeys { it.key.name }.toJsonObject())
        put("percentages", percentages.mapKeys { it.key.name }.toJsonObject())
        put("primaryDosha", primaryDosha)
        put("secondaryDosha", secondaryDosha)
        put("prakritiType", prakritiType)
        put("borderline", borderline)
        put("questionsAnswered", questionsAnswered)
    }
}

private fun String.toPrakritiResult(): PrakritiResult {
    val json = JSONObject(this)
    return PrakritiResult(
        scores = json.getJSONObject("scores").toDoshaIntMap(),
        percentages = json.getJSONObject("percentages").toDoshaDoubleMap(),
        primaryDosha = json.getString("primaryDosha"),
        secondaryDosha = json.optString("secondaryDosha").takeIf { it.isNotBlank() && it != "null" },
        prakritiType = json.getString("prakritiType"),
        borderline = json.getBoolean("borderline"),
        questionsAnswered = json.getInt("questionsAnswered")
    )
}

private fun Map<String, Any?>.toJsonObject(): JSONObject {
    return JSONObject().also { json ->
        forEach { (key, value) -> json.put(key, value) }
    }
}

private fun JSONObject.toDoshaIntMap(): Map<PrakritiDosha, Int> {
    return keys().asSequence().associate { key -> PrakritiDosha.valueOf(key) to getInt(key) }
}

private fun JSONObject.toDoshaDoubleMap(): Map<PrakritiDosha, Double> {
    return keys().asSequence().associate { key -> PrakritiDosha.valueOf(key) to getDouble(key) }
}
