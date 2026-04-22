package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

data class MbrdSaveEditorDraftCommand(
    val documentId: String?,
    val title: String,
    val author: String,
    val markdown: String,
    val tags: List<String>,
    val urlSlug: String?,
    val introduction: String?,
    val thumbnailImageUrl: String?,
    val status: String?,
)

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

data class MbrdEditorDocumentViewCountPayload(
    val viewCount: Long,
)

data class MbrdEditorBootstrapPayload(
    val draft: MbrdEditorDraftPayload,
    val suggestedTags: List<String>,
    val referenceImageUrl: String,
)

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

data class MbrdEditorImageUploadPayload(
    val imageUrl: String,
    val originalFileName: String,
    val storedFileName: String,
    val size: Long,
)

data class MbrdEditorImageContentPayload(
    val stream: java.io.InputStream,
    val contentType: String,
    val originalFileName: String,
    val size: Long,
)

data class MbrdEditorLiveSyncCommand(
    val sessionId: String?,
    val documentId: String?,
    val title: String?,
    val author: String?,
    val markdown: String?,
    val tags: List<String>?,
    val status: String?,
)

data class MbrdEditorLiveSyncPayload(
    val sessionId: String,
    val documentId: String,
    val title: String,
    val author: String,
    val markdown: String,
    val tags: List<String>,
    val status: String,
    val updatedAt: Instant,
)
