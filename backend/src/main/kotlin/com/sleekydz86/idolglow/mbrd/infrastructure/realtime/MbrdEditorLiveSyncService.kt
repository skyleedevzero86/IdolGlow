package com.sleekydz86.idolglow.mbrd.infrastructure.realtime

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorLiveSyncCommand
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorLiveSyncPayload
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentId
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentPublicationStatus
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class MbrdEditorLiveSyncService(
    private val clock: Clock,
) {
    fun publish(command: MbrdEditorLiveSyncCommand): MbrdEditorLiveSyncPayload {
        val normalizedTags = (command.tags ?: emptyList())
            .map { it.trim().replace("#", "").replace(",", "") }
            .filter { it.isNotBlank() }
            .distinct()
        return MbrdEditorLiveSyncPayload(
            sessionId = normalize(command.sessionId, "anonymous-session"),
            documentId = normalize(command.documentId, MbrdDocumentId.newId().asString()),
            title = trim(command.title),
            author = trim(command.author),
            markdown = command.markdown ?: "",
            tags = normalizedTags,
            status = normalizeStatus(command.status),
            updatedAt = clock.instant(),
        )
    }

    private fun normalize(value: String?, fallback: String): String {
        val t = trim(value)
        return if (t.isBlank()) fallback else t
    }

    private fun normalizeStatus(status: String?): String =
        MbrdDocumentPublicationStatus.fromApiValue(status).toApiValue()

    private fun trim(value: String?): String = value?.trim() ?: ""
}
