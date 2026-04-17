package com.sleekydz86.idolglow.mbrd.domain

import java.time.Instant
import java.util.UUID

data class MbrdEditorAsset(
    val id: UUID,
    val objectKey: String,
    val bucketName: String,
    val originalFileName: String,
    val contentType: String,
    val fileSize: Long,
    val uploadedAt: Instant,
)
