package com.example.vedaahar.assessment

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

enum class AssessmentSyncStatus { PENDING, SYNCING, SYNCED, FAILED }

data class AssessmentSyncRecord(
    val raw: String? = null,
    val submissionId: String = UUID.randomUUID().toString(),
    val revision: String = UUID.randomUUID().toString(),
    val status: AssessmentSyncStatus = AssessmentSyncStatus.PENDING,
    val ready: Boolean = false,
    val localChanges: Boolean = true,
    val editingComponents: Set<String> = emptySet(),
    val serverId: Long? = null,
    val serverVersion: String? = null,
    val message: String = "Complete Prakriti, Vikriti and Agni; results stay on this phone until synced."
)

interface AssessmentSyncCache {
    fun read(): AssessmentSyncRecord?
    fun replace(expected: AssessmentSyncRecord?, next: AssessmentSyncRecord, restoreComponents: Boolean = false): Boolean
}

class AssessmentSyncCoordinator(
    private val owner: String?,
    private val currentOwner: () -> String?,
    private val cache: AssessmentSyncCache,
    private val gateway: AssessmentGateway
) {
    private fun ownsSession() = owner != null && currentOwner() == owner

    suspend fun retry() { restoreLatest(); sync() }

    suspend fun sync() = NetworkLock.withLock {
        if (!ownsSession()) {
            val saved = cache.read()
            if (saved != null) cache.replace(saved, saved.copy(status = AssessmentSyncStatus.FAILED,
                message = "Sign in to the patient account that owns these results, then retry. Local results are saved."))
            return@withLock
        }
        val record = cache.read() ?: return@withLock
        if (!record.ready || record.raw == null || record.status == AssessmentSyncStatus.SYNCED) return@withLock
        val syncing = record.copy(status = AssessmentSyncStatus.SYNCING, message = "Syncing assessment with Django…")
        if (!cache.replace(record, syncing)) return@withLock
        try {
            val result = gateway.persistCanonicalAssessment(record.raw, record.submissionId)
            if (!ownsSession()) {
                cache.replace(syncing, syncing.copy(status = AssessmentSyncStatus.FAILED,
                    message = "Patient session expired or changed. Sign in to the original account and retry; local results are saved."))
                return@withLock
            }
            val next = when (result) {
                is AgenticApiResult.Success -> {
                    val remote = result.value
                    require(remote.id > 0 && remote.assessmentVersion?.matches(Regex("[0-9a-f]{64}")) == true) { "Backend did not return an assessment version." }
                    require(CanonicalAssessment.same(record.raw, CanonicalAssessment.raw(remote))) { "Backend acknowledged different assessment content." }
                    syncing.copy(status = AssessmentSyncStatus.SYNCED, localChanges = false,
                        serverId = remote.id, serverVersion = remote.assessmentVersion,
                        message = "Synced assessment #${remote.id} · version ${remote.assessmentVersion.take(12)}")
                }
                is AgenticApiResult.Error -> syncing.copy(status = AssessmentSyncStatus.FAILED, message = result.message)
            }
            // A late response must never acknowledge answers edited during the request.
            cache.replace(syncing, next)
        } catch (error: CancellationException) {
            cache.replace(syncing, syncing.copy(status = AssessmentSyncStatus.PENDING, message = "Sync interrupted. Local results are saved; retry sync."))
            throw error
        } catch (error: RuntimeException) {
            if (ownsSession()) cache.replace(syncing, syncing.copy(status = AssessmentSyncStatus.FAILED,
                message = "Assessment could not be verified: ${error.message ?: "invalid response"}. Local results are saved."))
        }
    }

    suspend fun restoreLatest() = NetworkLock.withLock {
        if (!ownsSession()) return@withLock
        val previous = cache.read()
        if (previous?.localChanges == true) return@withLock
        val before = (previous ?: AssessmentSyncRecord(localChanges = false)).copy(
            status = AssessmentSyncStatus.SYNCING, message = "Restoring latest assessment from Django…")
        if (!cache.replace(previous, before)) return@withLock
        val result = gateway.latestAssessment()
        if (!ownsSession()) {
            cache.replace(before, before.copy(status = AssessmentSyncStatus.FAILED,
                message = "Patient session expired or changed. Sign in to restore assessments."))
            return@withLock
        }
        when (result) {
            is AgenticApiResult.Success -> {
                if (!ownsSession()) return@withLock
                val remote = result.value
                try {
                    require(remote.id > 0 && remote.assessmentVersion?.matches(Regex("[0-9a-f]{64}")) == true) { "Backend assessment version is missing" }
                    val raw = CanonicalAssessment.raw(remote)
                    CanonicalAssessment.content(raw)
                    val restored = AssessmentSyncRecord(raw = raw, submissionId = remote.submissionId ?: UUID.randomUUID().toString(),
                        status = AssessmentSyncStatus.SYNCED, ready = true, localChanges = false,
                        serverId = remote.id, serverVersion = remote.assessmentVersion,
                        message = "Synced assessment #${remote.id} · version ${remote.assessmentVersion.take(12)}")
                    cache.replace(before, restored, restoreComponents = true)
                } catch (_: RuntimeException) {
                    cache.replace(before, before.copy(
                        status = AssessmentSyncStatus.FAILED, message = "The server assessment cannot be restored safely. Local answers were preserved."))
                }
            }
            is AgenticApiResult.Error -> {
                if (ownsSession()) {
                    if (result.type == AgenticApiErrorType.NotFound) {
                        cache.replace(before, before.copy(status = AssessmentSyncStatus.PENDING,
                            localChanges = before.raw != null,
                            message = "No server assessment found. Complete all three assessments or sync the local results."))
                    } else cache.replace(before, before.copy(status = AssessmentSyncStatus.FAILED, message = result.message))
                }
            }
        }
    }

    companion object { private val NetworkLock = Mutex() }
}
