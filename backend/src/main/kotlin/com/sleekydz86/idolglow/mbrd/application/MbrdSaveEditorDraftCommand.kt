package com.sleekydz86.idolglow.mbrd.application

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
