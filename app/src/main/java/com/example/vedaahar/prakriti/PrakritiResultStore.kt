package com.example.vedaahar.prakriti

import android.content.Context
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.assessment.AssessmentSyncStore
import com.example.vedaahar.assessment.AssessmentProfileStore
import org.json.JSONObject

object PrakritiResultStore {
    private const val PREF_NAME = "vedaahar_prakriti_result"
    private const val KEY_RESULT = "latest_result"

    fun save(context: Context, result: PrakritiResult, owner: String? = PatientSession.currentKey(context), trackChange: Boolean = true) {
        if (owner == null) return
        if (trackChange) AssessmentSyncStore(context, owner).markEditing("prakriti")
        PatientSession.preferences(context, PREF_NAME, owner)!!
            .edit()
            .putString(KEY_RESULT, result.toJson().toString())
            .apply()
        if (trackChange) {
            AssessmentSyncStore(context, owner).componentCompleted("prakriti")
            AssessmentProfileStore.stageIfComplete(context, owner)
        }
    }

    fun current(context: Context, owner: String? = PatientSession.currentKey(context)): PrakritiResult? {
        if (AssessmentSyncStore(context, owner).read()?.editingComponents?.contains("prakriti") == true) return null
        val raw = PatientSession.preferences(context, PREF_NAME, owner)?.getString(KEY_RESULT, null)
            ?: return null
        return runCatching { raw.toPrakritiResult() }.getOrNull()
    }

    fun clear(context: Context, owner: String? = PatientSession.currentKey(context)) {
        PatientSession.preferences(context, PREF_NAME, owner)
            ?.edit()
            ?.clear()
            ?.apply()
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

internal fun String.toPrakritiResult(): PrakritiResult {
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
