package com.sleekydz86.idolglow.mbrd.application

import java.time.Instant

data class MbrdEditorImageContentPayload(
    val stream: java.io.InputStream,
    val contentType: String,
    val originalFileName: String,
    val size: Long,
)
