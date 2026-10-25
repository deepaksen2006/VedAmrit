package com.example.vedaahar.assessment

import android.content.Context
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.prakriti.PrakritiResultStore
import com.example.vedaahar.vikriti.VikritiResultStore
import com.example.vedaahar.agni.AgniResultStore
import com.example.vedaahar.agni.AgniResult
import com.example.vedaahar.prakriti.PrakritiResult
import com.example.vedaahar.symptoms.SymptomsAnalysisResult
import com.example.vedaahar.vikriti.VikritiResult
import org.json.JSONArray
import org.json.JSONObject

object AssessmentProfileStore {
    fun save(
        context: Context,
        prakritiResult: PrakritiResult,
        vikritiResult: VikritiResult,
        agniResult: AgniResult,
        symptomsResult: SymptomsAnalysisResult? = null,
        owner: String? = PatientSession.currentKey(context)
    ) {
        val profile = JSONObject().apply {
            put("prakriti_result", prakritiResult.toJson())
            put("vikriti_result", vikritiResult.toJson())
            put("agni_result", agniResult.toJson())
            symptomsResult?.let { put("symptoms_result", it.toJson()) }
        }
        AssessmentSyncStore(context, owner).saveLocal(profile.toString())
    }

    fun stageIfComplete(context: Context, owner: String?) {
        val prakriti = PrakritiResultStore.current(context, owner) ?: return
        val vikriti = VikritiResultStore.current(context, owner) ?: return
        val agni = AgniResultStore.current(context, owner) ?: return
        save(context, prakriti, vikriti, agni, owner = owner)
    }

    fun currentRaw(context: Context): String? {
        return AssessmentSyncStore(context).read()?.raw
    }

    fun clear(context: Context, owner: String? = PatientSession.currentKey(context)) {
        AssessmentSyncStore(context, owner).markEditing()
        AssessmentDraftStore.clearAll(context, owner)
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

private fun SymptomsAnalysisResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("symptoms", symptoms.toJsonObject())
        put("reportedSymptoms", JSONArray(reportedSymptoms.map { it.key }))
        put("possibleCondition", possibleCondition)
        put("completedAtMillis", completedAtMillis)
    }
}

private fun Map<String, Any?>.toJsonObject(): JSONObject {
    return JSONObject().also { json ->
        forEach { (key, value) -> json.put(key, value) }
    }
}
