package com.sleekydz86.idolglow.event.ui.dto

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentSummaryPayload
import java.time.Instant

data class AdminEventSummaryResponse(
    val documentId: String,
    val title: String,
    val author: String,
    val introduction: String?,
    val thumbnailImageUrl: String?,
    val tags: List<String>,
    val status: String,
    val updatedAt: Instant,
    val viewCount: Long,
) {
    companion object {
        fun from(payload: MbrdEditorDocumentSummaryPayload): AdminEventSummaryResponse =
            AdminEventSummaryResponse(
                documentId = payload.documentId,
                title = payload.title,
                author = payload.author,
                introduction = payload.introduction,
                thumbnailImageUrl = payload.thumbnailImageUrl,
                tags = payload.tags,
                status = payload.status,
                updatedAt = payload.updatedAt,
                viewCount = payload.viewCount,
            )
    }
}
