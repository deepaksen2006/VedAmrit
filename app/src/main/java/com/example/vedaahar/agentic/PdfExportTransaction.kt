package com.example.vedaahar.agentic

import java.io.IOException
import java.io.OutputStream

enum class PublicPdfRoute { MediaStoreDownloads, CreateDocument }
fun publicPdfRoute(sdk: Int) = if (sdk >= 29) PublicPdfRoute.MediaStoreDownloads else PublicPdfRoute.CreateDocument

/** Each destination is a newly created entry, never an existing unrelated file. */
interface PendingPdfDestination {
    fun create(displayName: String): String
    fun output(id: String): OutputStream
    fun publish(id: String)
    fun delete(id: String)
}

fun publishVerifiedPdf(
    data: ByteArray, displayName: String, destination: PendingPdfDestination, authorize: () -> Unit
): String {
    require(data.size <= DraftPdfRepository.MAX_BYTES && DraftPdfRepository.isPdf(data)) { "Invalid PDF response." }
    require(displayName.matches(Regex("VedAmrit_Diet_Plan_[0-9]+_v[0-9]+_[0-9]+_[a-f0-9]+\\.pdf"))) { "Invalid export filename." }
    authorize()
    val id = destination.create(displayName)
    try {
        destination.output(id).use { output ->
            var offset = 0
            while (offset < data.size) {
                authorize()
                val count = minOf(8192, data.size - offset)
                output.write(data, offset, count)
                offset += count
            }
            output.flush()
        } // A close/write failure must never publish the entry.
        authorize()
        destination.publish(id)
        authorize()
        return id
    } catch (failure: Throwable) {
        try { destination.delete(id) }
        catch (cleanup: Throwable) {
            throw IOException("Public PDF save failed and the incomplete entry could not be removed. Check device storage.", failure)
                .apply { addSuppressed(cleanup) }
        }
        throw failure
    }
}

