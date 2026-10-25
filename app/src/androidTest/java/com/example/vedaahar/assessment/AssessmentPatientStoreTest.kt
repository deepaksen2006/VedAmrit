package com.example.vedaahar.assessment

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vedaahar.network.AuthTokenStore
import com.example.vedaahar.network.PatientSession
import com.example.vedaahar.prakriti.*
import com.example.vedaahar.vikriti.*
import com.example.vedaahar.agni.*
import com.example.vedaahar.agentic.AgenticPlanRequestMapper
import com.example.vedaahar.agentic.AgenticRequestMappingResult
import com.example.vedaahar.profile.PatientProfileStore
import com.example.vedaahar.profile.StoredPatientProfile
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AssessmentPatientStoreTest {
    private val real = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = mutableSetOf<String>()
    private val namespace = "assessment_isolation_test_${UUID.randomUUID()}_"
    // All preferences, including test JWTs, are redirected away from patient data.
    private val context = object : ContextWrapper(real) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val testName = "$namespace$name"
            files += testName
            return real.getSharedPreferences(testName, mode)
        }
    }
    private val prakriti get() = generatePrakritiResult(prakritiQuestions.associate { it.id to "A" })
    private val vikriti get() = generateVikritiResult(vikritiQuestions.associate { it.id to "not_at_all" }, "Vata")
    private val agni get() = generateAgniResult(agniQuestions.associate { it.id to it.options.first().id })

    private fun signIn(id: Int) {
        val payload = Base64.encodeToString("{\"user_id\":$id}".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        AuthTokenStore(context).saveTokens("test-header.$payload.test-signature", "test-refresh-$id")
    }
    private fun complete() {
        PrakritiResultStore.save(context, prakriti)
        VikritiResultStore.save(context, vikriti)
        AgniResultStore.save(context, agni)
    }

    @Before fun setUp() { signIn(1) }
    @After fun tearDown() {
        files.forEach {
            // Drain any earlier apply() writes before deleting this test's files.
            real.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
            real.deleteSharedPreferences(it)
        }
    }

    @Test fun partialResultsStayPendingAndCompletionCreatesDurableCanonicalSubmission() {
        PrakritiResultStore.save(context, prakriti)
        assertFalse(AssessmentSyncStore(context).read()!!.ready)
        VikritiResultStore.save(context, vikriti)
        assertFalse(AssessmentSyncStore(context).read()!!.ready)
        AgniResultStore.save(context, agni)
        val saved = AssessmentSyncStore(context).read()!!
        assertEquals(AssessmentSyncStatus.PENDING, saved.status)
        assertTrue(saved.ready)
        assertEquals(setOf("prakriti", "vikriti", "agni"), CanonicalAssessment.content(saved.raw!!).keys)
        assertEquals(saved, AssessmentSyncStore(context).read())
        AssessmentProfileStore.stageIfComplete(context, PatientSession.currentKey(context))
        assertEquals(saved.submissionId, AssessmentSyncStore(context).read()!!.submissionId)
    }

    @Test fun accountsHaveSeparateResultsAndPendingSubmissionIds() {
        complete()
        val first = AssessmentSyncStore(context).read()!!
        signIn(2)
        assertNull(PrakritiResultStore.current(context))
        assertNull(VikritiResultStore.current(context))
        assertNull(AgniResultStore.current(context))
        assertNull(AssessmentProfileStore.currentRaw(context))
        complete()
        assertNotEquals(first.submissionId, AssessmentSyncStore(context).read()!!.submissionId)
        signIn(1)
        assertEquals(first, AssessmentSyncStore(context).read())
        assertEquals(prakriti, PrakritiResultStore.current(context))
    }

    @Test fun unownedLegacyResultsAreNotAssignedToNextPatient() {
        context.getSharedPreferences("vedaahar_prakriti_result", Context.MODE_PRIVATE).edit()
            .putString("latest_result", "unowned legacy assessment").commit()
        assertNull(PrakritiResultStore.current(context))
        assertEquals("unowned legacy assessment", context.getSharedPreferences("vedaahar_prakriti_result", Context.MODE_PRIVATE).getString("latest_result", null))
    }

    @Test fun typedRemoteRestoreDoesNotRescoreAndLateRestoreCannotOverwriteEdits() {
        complete()
        val original = AssessmentSyncStore(context).read()!!
        val owner = PatientSession.currentKey(context)
        // A clean server-only cache for the same test account.
        PatientSession.preferences(context, "vedaahar_assessment_sync", owner)!!.edit().clear().commit()
        val store = AssessmentSyncStore(context, owner)
        val restored = original.copy(status = AssessmentSyncStatus.SYNCED, localChanges = false, serverId = 7, serverVersion = "a".repeat(64))
        assertTrue(store.replace(null, restored, restoreComponents = true))
        assertEquals(prakriti, PrakritiResultStore.current(context))
        assertEquals(vikriti.scores, VikritiResultStore.current(context)!!.scores)
        assertEquals(agni.scores, AgniResultStore.current(context)!!.scores)
        store.markEditing("prakriti")
        assertFalse(store.replace(restored, restored.copy(serverId = 8), restoreComponents = true))
        assertEquals(AssessmentSyncStatus.PENDING, store.read()!!.status)
    }

    @Test fun sessionExpiryDoesNotDiscardResultsCompletedForCapturedPatient() {
        complete()
        val owner = PatientSession.currentKey(context)!!
        AuthTokenStore(context).clear()
        PrakritiResultStore.save(context, prakriti, owner)
        assertTrue(AssessmentSyncStore(context, owner).read()!!.ready)
        assertNull(AssessmentSyncStore(context).read())
        signIn(1)
        assertNotNull(AssessmentSyncStore(context).read()!!.raw)
    }

    @Test fun plannerMapperRequiresSyncedPatientAndExactAssessmentReference() {
        complete()
        PatientProfileStore.save(context, StoredPatientProfile("Test patient", "34", "Female", "164", "58",
            dietaryPreference = "vegetarian", goal = "general wellness", activityLevel = "moderate",
            allergies = emptyList(), foodIntolerances = emptyList(), foodsToAvoid = emptyList(),
            healthConditions = emptyList(), medicineAllergies = emptyList(), medicationRestrictions = emptyList(),
            doctorRestrictions = emptyList(), pregnancyInformation = "not_applicable"))
        assertTrue(AgenticPlanRequestMapper.fromStoredState(context) is AgenticRequestMappingResult.MissingData)
        val store = AssessmentSyncStore(context)
        val pending = store.read()!!
        store.replace(pending, pending.copy(status = AssessmentSyncStatus.SYNCED, localChanges = false,
            serverId = 7, serverVersion = "a".repeat(64)))
        val request = (AgenticPlanRequestMapper.fromStoredState(context) as AgenticRequestMappingResult.Success).request
        assertEquals(7L, request.assessmentId)
        assertEquals("a".repeat(64), request.assessmentVersion)
        signIn(2)
        assertTrue(AgenticPlanRequestMapper.fromStoredState(context) is AgenticRequestMappingResult.MissingData)
        assertEquals("", PatientProfileStore.current(context).fullName)
    }

    @Test fun unfinishedAnswersSurviveRecreationAndStayWithTheirPatient() {
        val owner = PatientSession.currentKey(context)!!
        AssessmentDraftStore.save(context, "prakriti", mapOf("1" to "A", "2" to "B"), owner)
        assertEquals(mapOf(1 to "A", 2 to "B"), AssessmentDraftStore.intAnswers(context, "prakriti", owner))
        assertTrue(AssessmentSyncStore(context).read()!!.localChanges)
        assertFalse(AssessmentSyncStore(context).read()!!.ready)
        signIn(2)
        assertTrue(AssessmentDraftStore.answers(context, "prakriti", PatientSession.currentKey(context)).isEmpty())
        signIn(1)
        assertEquals(2, AssessmentDraftStore.answers(context, "prakriti", PatientSession.currentKey(context)).size)
        PrakritiResultStore.save(context, prakriti)
        assertTrue(AssessmentDraftStore.answers(context, "prakriti", owner).isEmpty())
    }
}
