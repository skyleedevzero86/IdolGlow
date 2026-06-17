package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

data class MbrdEditorDraftPayload(
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
    val previousDocument: MbrdEditorDocumentSummaryPayload?,
    val nextDocument: MbrdEditorDocumentSummaryPayload?,
)
