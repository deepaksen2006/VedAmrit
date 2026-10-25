package com.example.vedaahar.agentic

import android.content.Context
import com.example.vedaahar.network.PatientSession

object AgenticPlanStore {
    private const val PrefName = "vedaahar_agentic_plan"
    private const val LatestPlanId = "latest_plan_id"

    fun saveLatestPlanId(context: Context, planId: Long) {
        (PatientSession.preferences(context, PrefName) ?: return)
            .edit()
            .putLong(LatestPlanId, planId)
            .apply()
    }

    fun latestPlanId(context: Context): Long? {
        val preferences = PatientSession.preferences(context, PrefName) ?: return null
        if (!preferences.contains(LatestPlanId)) return null
        return preferences.getLong(LatestPlanId, 0L).takeIf { it > 0L }
    }
}
