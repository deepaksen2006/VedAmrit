package com.example.vedaahar.agentic

import android.content.Context

object AgenticPlanStore {
    private const val PrefName = "vedaahar_agentic_plan"
    private const val LatestPlanId = "latest_plan_id"

    fun saveLatestPlanId(context: Context, planId: Long) {
        context.getSharedPreferences(PrefName, Context.MODE_PRIVATE)
            .edit()
            .putLong(LatestPlanId, planId)
            .apply()
    }

    fun latestPlanId(context: Context): Long? {
        val preferences = context.getSharedPreferences(PrefName, Context.MODE_PRIVATE)
        if (!preferences.contains(LatestPlanId)) return null
        return preferences.getLong(LatestPlanId, 0L).takeIf { it > 0L }
    }
}
