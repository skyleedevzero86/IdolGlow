package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

data class MbrdEditorBootstrapPayload(
    val draft: MbrdEditorDraftPayload,
    val suggestedTags: List<String>,
    val referenceImageUrl: String,
)
