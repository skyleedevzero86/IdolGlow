package com.sleekydz86.idolglow.event.application.dto

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentSummaryPayload
import com.sleekydz86.idolglow.mbrd.application.MbrdEditorDraftPayload
import java.time.Instant

data class AdminEventPageResult(
    val items: List<AdminEventSummaryResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class AdminEventSummaryResult(
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
        fun from(payload: MbrdEditorDocumentSummaryPayload): AdminEventSummaryResult =
            AdminEventSummaryResult(
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

data class AdminEventDetailResult(
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
        fun from(payload: MbrdEditorDraftPayload): AdminEventDetailResult =
            AdminEventDetailResult(
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

fun com.sleekydz86.idolglow.mbrd.application.MbrdEditorDocumentPagePayload.toAdminEventPageResult(): AdminEventPageResult =
    AdminEventPageResult(
        items = content.map(AdminEventSummaryResult::from),
        page = page + 1,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages.coerceAtLeast(1),
    )
