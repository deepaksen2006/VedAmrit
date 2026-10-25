package com.example.vedaahar.assessment

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.dto.AssessmentResultResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AssessmentSyncCoordinatorTest {
    private val raw = """{"prakriti_result":{"scores":{"Vata":10},"prakritiType":"Vata"},"vikriti_result":{"scores":{"Pitta":8},"dominantDosha":"Pitta"},"agni_result":{"scores":{"Samagni":6},"dominantAgni":"Samagni"}}"""
    private val version = "a".repeat(64)
    private fun response(id: Long = 4) = CanonicalAssessment.content(raw).let { content ->
        @Suppress("UNCHECKED_CAST")
        AssessmentResultResponse(id, content["prakriti"] as Map<String, Any?>,
            content["vikriti"] as Map<String, Any?>, content["agni"] as Map<String, Any?>,
            null, null, null, version)
    }
    private fun pending() = AssessmentSyncRecord(raw = raw, ready = true)
    private class MemoryCache(var record: AssessmentSyncRecord?) : AssessmentSyncCache {
        var restored = false
        override fun read() = record
        override fun replace(expected: AssessmentSyncRecord?, next: AssessmentSyncRecord, restoreComponents: Boolean): Boolean {
            if (record != expected) return false
            record = next
            restored = restoreComponents
            return true
        }
    }
    private class Gateway : AssessmentGateway {
        val submissions = mutableListOf<Pair<String, String?>>()
        var latestCalls = 0
        lateinit var post: suspend () -> AgenticApiResult<AssessmentResultResponse>
        lateinit var get: suspend () -> AgenticApiResult<AssessmentResultResponse>
        override suspend fun persistCanonicalAssessment(rawAssessmentProfile: String, submissionId: String?): AgenticApiResult<AssessmentResultResponse> {
            submissions += rawAssessmentProfile to submissionId
            return post()
        }
        override suspend fun latestAssessment(): AgenticApiResult<AssessmentResultResponse> {
            latestCalls++
            return get()
        }
    }

    @Test fun successfulSyncIncludesThreeResultsAndPinsServerVersion() = runTest {
        val cache = MemoryCache(pending())
        val gateway = Gateway().apply { post = { AgenticApiResult.Success(response()) } }
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).sync()
        assertEquals(AssessmentSyncStatus.SYNCED, cache.record?.status)
        assertEquals(4L, cache.record?.serverId)
        assertEquals(version, cache.record?.serverVersion)
        assertEquals(setOf("prakriti", "vikriti", "agni"), CanonicalAssessment.content(gateway.submissions.single().first).keys)
    }

    @Test fun timeoutRetryReusesSubmissionIdAndPreservesLocalResults() = runTest {
        val original = pending()
        val cache = MemoryCache(original)
        val gateway = Gateway().apply { post = { AgenticApiResult.Error(AgenticApiErrorType.Timeout, "Timed out") } }
        val sync = AssessmentSyncCoordinator("A", { "A" }, cache, gateway)
        sync.sync()
        assertEquals(AssessmentSyncStatus.FAILED, cache.record?.status)
        assertEquals(raw, cache.record?.raw)
        gateway.post = { AgenticApiResult.Success(response()) }
        sync.sync()
        assertEquals(listOf(original.submissionId, original.submissionId), gateway.submissions.map { it.second })
        assertEquals(AssessmentSyncStatus.SYNCED, cache.record?.status)
    }

    @Test fun offlineValidationAndExpiredJwtFailuresRemainRetryable() = runTest {
        for (type in listOf(AgenticApiErrorType.Network, AgenticApiErrorType.BadRequest, AgenticApiErrorType.AuthenticationExpired)) {
            val cache = MemoryCache(pending())
            val gateway = Gateway().apply { post = { AgenticApiResult.Error(type, "Visible failure: $type") } }
            AssessmentSyncCoordinator("A", { "A" }, cache, gateway).sync()
            assertEquals(AssessmentSyncStatus.FAILED, cache.record?.status)
            assertEquals(raw, cache.record?.raw)
            assertTrue(cache.record!!.message.contains(type.toString()))
        }
    }

    @Test fun partialResultsCannotBeSubmitted() = runTest {
        val cache = MemoryCache(pending().copy(ready = false))
        val gateway = Gateway()
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).sync()
        assertTrue(gateway.submissions.isEmpty())
    }

    @Test fun lateAcknowledgementCannotMarkNewAnswersSynced() = runTest {
        val cache = MemoryCache(pending())
        val newer = pending().copy(raw = raw.replace("10", "11"))
        val gateway = Gateway().apply { post = { cache.record = newer; AgenticApiResult.Success(response()) } }
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).sync()
        assertEquals(newer, cache.record)
    }

    @Test fun serverCannotAcknowledgeDifferentContent() = runTest {
        val cache = MemoryCache(pending())
        val gateway = Gateway().apply { post = { AgenticApiResult.Success(response().copy(prakritiResult = mapOf("scores" to mapOf("Vata" to 99), "prakritiType" to "Vata"))) } }
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).sync()
        assertEquals(AssessmentSyncStatus.FAILED, cache.record?.status)
        assertEquals(raw, cache.record?.raw)
    }

    @Test fun unsyncedAnswersAlwaysWinOverAutomaticRestore() = runTest {
        val local = pending()
        val cache = MemoryCache(local)
        val gateway = Gateway()
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).restoreLatest()
        assertEquals(0, gateway.latestCalls)
        assertEquals(local, cache.record)
    }

    @Test fun restoreDoesNotOverwriteAnswersCreatedWhileGetWasInFlight() = runTest {
        val cache = MemoryCache(null)
        val newer = pending()
        val gateway = Gateway().apply { get = { cache.record = newer; AgenticApiResult.Success(response()) } }
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).restoreLatest()
        assertEquals(newer, cache.record)
        assertFalse(cache.restored)
    }

    @Test fun cleanCacheRestoresLatestPersistedAssessment() = runTest {
        val cache = MemoryCache(null)
        val gateway = Gateway().apply { get = { AgenticApiResult.Success(response(9)) } }
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).restoreLatest()
        assertTrue(cache.restored)
        assertEquals(9L, cache.record?.serverId)
        assertEquals(AssessmentSyncStatus.SYNCED, cache.record?.status)
    }

    @Test fun switchingAccountDuringPostDoesNotAcknowledgeOrSubmitForNewPatient() = runTest {
        var active = "A"
        val cache = MemoryCache(pending())
        val gateway = Gateway().apply { post = { active = "B"; AgenticApiResult.Success(response()) } }
        val sync = AssessmentSyncCoordinator("A", { active }, cache, gateway)
        sync.sync()
        assertNotEquals(AssessmentSyncStatus.SYNCED, cache.record?.status)
        sync.sync()
        assertEquals(1, gateway.submissions.size)
        assertEquals(raw, cache.record?.raw)
    }

    @Test fun interruptedSyncOnProcessRestartReusesPersistedSubmissionId() = runTest {
        val saved = pending().copy(status = AssessmentSyncStatus.SYNCING)
        val cache = MemoryCache(saved)
        val gateway = Gateway().apply { post = { AgenticApiResult.Success(response()) } }
        AssessmentSyncCoordinator("A", { "A" }, cache, gateway).sync()
        assertEquals(saved.submissionId, gateway.submissions.single().second)
        assertEquals(AssessmentSyncStatus.SYNCED, cache.record?.status)
    }

    @Test fun refreshRejectionDuringSyncShowsFailureInsteadOfStickingInSyncing() = runTest {
        var active: String? = "A"
        val cache = MemoryCache(pending())
        val gateway = Gateway().apply { post = {
            active = null
            AgenticApiResult.Error(AgenticApiErrorType.AuthenticationExpired, "Expired")
        } }
        AssessmentSyncCoordinator("A", { active }, cache, gateway).sync()
        assertEquals(AssessmentSyncStatus.FAILED, cache.record?.status)
        assertEquals(raw, cache.record?.raw)
        assertTrue(cache.record!!.message.contains("Sign in"))
    }
}
