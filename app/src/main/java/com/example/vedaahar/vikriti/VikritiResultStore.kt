package com.example.vedaahar.vikriti

import android.content.Context
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.assessment.AssessmentSyncStore
import com.example.vedaahar.assessment.AssessmentProfileStore
import org.json.JSONObject

object VikritiResultStore {
    private const val PREF_NAME = "vedaahar_vikriti_result"
    private const val KEY_RESULT = "latest_result"

    fun save(context: Context, result: VikritiResult, owner: String? = PatientSession.currentKey(context), trackChange: Boolean = true) {
        if (owner == null) return
        if (trackChange) AssessmentSyncStore(context, owner).markEditing("vikriti")
        PatientSession.preferences(context, PREF_NAME, owner)!!
            .edit()
            .putString(KEY_RESULT, result.toJson().toString())
            .apply()
        if (trackChange) {
            AssessmentSyncStore(context, owner).componentCompleted("vikriti")
            AssessmentProfileStore.stageIfComplete(context, owner)
        }
    }

    fun current(context: Context, owner: String? = PatientSession.currentKey(context)): VikritiResult? {
        if (AssessmentSyncStore(context, owner).read()?.editingComponents?.contains("vikriti") == true) return null
        val raw = PatientSession.preferences(context, PREF_NAME, owner)?.getString(KEY_RESULT, null)
            ?: return null
        return runCatching { raw.toVikritiResult() }.getOrNull()
    }

    fun clear(context: Context, owner: String? = PatientSession.currentKey(context)) {
        PatientSession.preferences(context, PREF_NAME, owner)
            ?.edit()
            ?.clear()
            ?.apply()
    }
}

private fun VikritiResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("answers", answers.toJsonObject())
        put("scores", scores.mapKeys { it.key.name }.toJsonObject())
        put("severities", severities.mapKeys { it.key.name }.toJsonObject())
        put("percentages", percentages.mapKeys { it.key.name }.toJsonObject())
        put("dominantDosha", dominantDosha)
        put("completedAtMillis", completedAtMillis)
        put("prakritiType", prakritiType)
    }
}

internal fun String.toVikritiResult(): VikritiResult {
    val json = JSONObject(this)
    return VikritiResult(
        answers = json.getJSONObject("answers").toStringMap(),
        scores = json.getJSONObject("scores").toDoshaIntMap(),
        severities = json.getJSONObject("severities").toDoshaStringMap(),
        percentages = json.getJSONObject("percentages").toDoshaDoubleMap(),
        dominantDosha = json.getString("dominantDosha"),
        completedAtMillis = json.getLong("completedAtMillis"),
        prakritiType = json.getString("prakritiType")
    )
}

private fun Map<String, Any>.toJsonObject(): JSONObject {
    return JSONObject().also { json ->
        forEach { (key, value) -> json.put(key, value) }
    }
}

private fun JSONObject.toStringMap(): Map<String, String> {
    return keys().asSequence().associateWith { key -> getString(key) }
}

private fun JSONObject.toDoshaIntMap(): Map<VikritiDosha, Int> {
    return keys().asSequence().associate { key -> VikritiDosha.valueOf(key) to getInt(key) }
}

private fun JSONObject.toDoshaStringMap(): Map<VikritiDosha, String> {
    return keys().asSequence().associate { key -> VikritiDosha.valueOf(key) to getString(key) }
}

private fun JSONObject.toDoshaDoubleMap(): Map<VikritiDosha, Double> {
    return keys().asSequence().associate { key -> VikritiDosha.valueOf(key) to getDouble(key) }
}
