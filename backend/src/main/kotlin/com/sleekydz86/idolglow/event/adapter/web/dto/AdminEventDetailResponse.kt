package com.sleekydz86.idolglow.event.adapter.web.dto

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDraftPayload
import java.time.Instant

data class AdminEventDetailResponse(
    val documentId: String,
    val title: String,
    val author: String,
    val markdown: String,
    val tags: List<String>,
    val urlSlug: String?,
    val introduction: String?,
    val thumbnailImageUrl: String?,
    val status: String,
    val updatedAt: Instant,
    val viewCount: Long,
) {
    companion object {
        fun from(payload: MbrdEditorDraftPayload): AdminEventDetailResponse =
            AdminEventDetailResponse(
                documentId = payload.documentId,
                title = payload.title,
                author = payload.author,
                markdown = payload.markdown,
                tags = payload.tags,
                urlSlug = payload.urlSlug,
                introduction = payload.introduction,
                thumbnailImageUrl = payload.thumbnailImageUrl,
                status = payload.status,
                updatedAt = payload.updatedAt,
                viewCount = payload.viewCount,
            )
    }
}
