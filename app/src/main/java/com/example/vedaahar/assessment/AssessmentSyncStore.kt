package com.example.vedaahar.assessment

import android.content.Context
import android.content.SharedPreferences
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.prakriti.PrakritiResultStore
import com.example.vedaahar.prakriti.toPrakritiResult
import com.example.vedaahar.vikriti.VikritiResultStore
import com.example.vedaahar.vikriti.toVikritiResult
import com.example.vedaahar.agni.AgniResultStore
import com.example.vedaahar.agni.toAgniResult
import com.google.gson.Gson
import org.json.JSONObject

class AssessmentSyncStore(private val context: Context, private val owner: String? = PatientSession.currentKey(context)) : AssessmentSyncCache {
    private val preferences = PatientSession.preferences(context, "vedaahar_assessment_sync", owner)

    override fun read(): AssessmentSyncRecord? = synchronized(StorageLock) {
        preferences?.getString("record", null)?.let { Gson().fromJson(it, AssessmentSyncRecord::class.java) }
    }

    override fun replace(expected: AssessmentSyncRecord?, next: AssessmentSyncRecord, restoreComponents: Boolean): Boolean = synchronized(StorageLock) {
        if (preferences == null || read() != expected) return false
        if (restoreComponents) {
            val root = JSONObject(requireNotNull(next.raw))
            // Decode all three first. A malformed remote result cannot partially replace local answers.
            val prakriti = root.getJSONObject("prakriti_result").toString().toPrakritiResult()
            val vikriti = root.getJSONObject("vikriti_result").toString().toVikritiResult()
            val agni = root.getJSONObject("agni_result").toString().toAgniResult()
            PrakritiResultStore.save(context, prakriti, owner, trackChange = false)
            VikritiResultStore.save(context, vikriti, owner, trackChange = false)
            AgniResultStore.save(context, agni, owner, trackChange = false)
            AssessmentDraftStore.clearAll(context, owner)
        }
        preferences.edit().putString("record", Gson().toJson(next)).commit()
    }

    fun saveLocal(raw: String): Unit = synchronized(StorageLock) {
        CanonicalAssessment.content(raw)
        val previous = read()
        if (!previous?.editingComponents.isNullOrEmpty()) return
        if (previous?.ready == true && previous.raw != null && CanonicalAssessment.same(previous.raw, raw)) return
        replace(previous, AssessmentSyncRecord(raw = raw, ready = true, message = "Pending sync. All three assessments are saved locally."))
    }

    fun markEditing(component: String? = null) = synchronized(StorageLock) {
        val previous = read()
        val editing = previous?.editingComponents.orEmpty() + (component?.let { setOf(it) } ?: setOf("prakriti", "vikriti", "agni"))
        replace(previous, AssessmentSyncRecord(raw = previous?.raw, editingComponents = editing,
            message = "Pending: complete the assessment changes before syncing."))
    }

    fun componentCompleted(component: String) = synchronized(StorageLock) {
        val previous = read() ?: AssessmentSyncRecord()
        AssessmentDraftStore.clear(context, component, owner)
        replace(read(), previous.copy(editingComponents = previous.editingComponents - component))
    }

    fun observe(onChange: () -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
        preferences?.registerOnSharedPreferenceChangeListener(listener)
        return { preferences?.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object { private val StorageLock = Any() }
}
