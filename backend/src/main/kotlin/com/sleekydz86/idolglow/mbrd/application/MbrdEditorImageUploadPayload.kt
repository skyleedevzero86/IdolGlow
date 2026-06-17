package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

data class MbrdEditorImageUploadPayload(
    val imageUrl: String,
    val originalFileName: String,
    val storedFileName: String,
    val size: Long,
)
