package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

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
