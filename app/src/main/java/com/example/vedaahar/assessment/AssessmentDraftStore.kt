package com.example.vedaahar.assessment

import android.content.Context
import com.example.vedaahar.network.PatientSession
import org.json.JSONObject

object AssessmentDraftStore {
    fun answers(context: Context, component: String, owner: String?): Map<String, String> {
        val raw = PatientSession.preferences(context, "vedaahar_assessment_drafts", owner)?.getString(component, null) ?: return emptyMap()
        return runCatching { JSONObject(raw).let { json -> json.keys().asSequence().associateWith { json.getString(it) } } }.getOrDefault(emptyMap())
    }

    fun intAnswers(context: Context, component: String, owner: String?): Map<Int, String> =
        answers(context, component, owner).mapKeys { it.key.toInt() }

    fun save(context: Context, component: String, answers: Map<String, String>, owner: String?) {
        if (owner == null) return
        AssessmentSyncStore(context, owner).markEditing(component)
        PatientSession.preferences(context, "vedaahar_assessment_drafts", owner)?.edit()
            ?.putString(component, JSONObject(answers).toString())?.commit()
    }

    fun clear(context: Context, component: String, owner: String?) {
        PatientSession.preferences(context, "vedaahar_assessment_drafts", owner)?.edit()?.remove(component)?.commit()
    }

    fun clearAll(context: Context, owner: String?) {
        PatientSession.preferences(context, "vedaahar_assessment_drafts", owner)?.edit()?.clear()?.commit()
    }
}
