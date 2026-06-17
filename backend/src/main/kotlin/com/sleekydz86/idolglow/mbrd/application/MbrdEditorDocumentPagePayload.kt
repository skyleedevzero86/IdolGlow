package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

data class MbrdEditorDocumentPagePayload(
    val content: List<MbrdEditorDocumentSummaryPayload>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val query: String,
    val statusFilter: String,
)
