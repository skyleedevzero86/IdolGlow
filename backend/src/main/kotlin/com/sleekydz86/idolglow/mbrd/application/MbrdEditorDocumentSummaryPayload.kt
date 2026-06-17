package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

data class MbrdEditorDocumentSummaryPayload(
    val documentId: String,
    val title: String,
    val author: String,
    val introduction: String?,
    val thumbnailImageUrl: String?,
    val tags: List<String>,
    val status: String,
    val updatedAt: Instant,
    val viewCount: Long,
)
