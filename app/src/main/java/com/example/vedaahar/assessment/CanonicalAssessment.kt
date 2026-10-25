package com.example.vedaahar.assessment

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.example.vedaahar.network.dto.AssessmentResultResponse
import org.json.JSONArray
import org.json.JSONObject

object CanonicalAssessment {
    fun content(raw: String): Map<String, Any?> {
        val root = JSONObject(raw)
        val block = root.optJSONObject("assessment") ?: root
        return listOf("prakriti", "vikriti", "agni").associateWith { name ->
            val result = block.optJSONObject(name) ?: block.optJSONObject("${name}_result")
                ?: error("Complete Prakriti, Vikriti and Agni before syncing.")
            require(result.optJSONObject("scores") != null) { "$name scores are missing." }
            val fields = when (name) {
                "prakriti" -> listOf("prakritiType", "constitution", "primaryDosha", "primary_dosha")
                "vikriti" -> listOf("dominantDosha", "dominant_dosha")
                else -> listOf("dominantAgni", "status", "displayResult")
            }
            require(fields.any { !result.optString(it).isNullOrBlank() && result.optString(it) != "null" }) { "$name result is missing." }
            result.toMap()
        }
    }

    fun same(first: String, second: String): Boolean =
        JsonParser.parseString(Gson().toJson(content(first))) == JsonParser.parseString(Gson().toJson(content(second)))

    fun raw(response: AssessmentResultResponse): String = Gson().toJson(mapOf(
        "prakriti_result" to response.prakritiResult,
        "vikriti_result" to response.vikritiResult,
        "agni_result" to response.agniResult
    ))

    private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { value(get(it)) }
    private fun value(item: Any?): Any? = when (item) {
        is JSONObject -> item.toMap()
        is JSONArray -> (0 until item.length()).map { value(item.get(it)) }
        JSONObject.NULL -> null
        else -> item
    }
}
