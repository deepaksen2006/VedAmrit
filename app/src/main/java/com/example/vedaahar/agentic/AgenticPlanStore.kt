package com.example.vedaahar.agentic

import android.content.Context
import com.example.vedaahar.network.PatientSession

object AgenticPlanStore {
    private const val PrefName = "vedaahar_agentic_plan"
    private const val LatestPlanId = "latest_plan_id"

    fun generation(context: Context, signature: String): String? {
        val prefs = PatientSession.preferences(context, PrefName) ?: return null
        return prefs.getString("generation_id", null).takeIf { prefs.getString("generation_inputs", null) == signature }
    }

    fun pendingGeneration(context: Context, signature: String): String? =
        generation(context, signature).takeUnless {
            PatientSession.preferences(context, PrefName)?.getBoolean("generation_complete", false) == true
        }

    fun generationFor(context: Context, signature: String): String {
        generation(context, signature)?.let { return it }
        val id = java.util.UUID.randomUUID().toString()
        val prefs = PatientSession.preferences(context, PrefName) ?: error("Sign in before generating a plan.")
        check(prefs.edit().putString("generation_id", id).putString("generation_inputs", signature)
            .putBoolean("generation_complete", false).commit()) {
            "Could not save the generation attempt. Free storage and retry."
        }
        return id
    }

    fun clearGeneration(context: Context) {
        PatientSession.preferences(context, PrefName)?.edit()?.remove("generation_id")?.remove("generation_inputs")?.commit()
    }

    fun saveLatestPlanId(context: Context, planId: Long) {
        (PatientSession.preferences(context, PrefName) ?: return)
            .edit()
            .putLong(LatestPlanId, planId)
            .putBoolean("generation_complete", true)
            .apply()
    }

    fun latestPlanId(context: Context): Long? {
        val preferences = PatientSession.preferences(context, PrefName) ?: return null
        if (!preferences.contains(LatestPlanId)) return null
        return preferences.getLong(LatestPlanId, 0L).takeIf { it > 0L }
    }
}
