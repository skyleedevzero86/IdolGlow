package com.sleekydz86.idolglow.mbrd.application

data class MbrdEditorLiveSyncCommand(
    val sessionId: String?,
    val documentId: String?,
    val title: String?,
    val author: String?,
    val markdown: String?,
    val tags: List<String>?,
    val status: String?,
)
