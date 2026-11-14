package com.sleekydz86.idolglow.history.application.dto

import com.sleekydz86.idolglow.global.support.ContentTargetType
import java.time.LocalDateTime

data class RecordContentViewCommand(
    val userId: Long,
    val contentType: ContentTargetType,
    val contentKey: String,
    val title: String?,
    val path: String?,
    val imageUrl: String? = null,
)

data class ContentViewResult(
    val id: Long,
    val contentType: String,
    val contentKey: String,
    val title: String,
    val path: String,
    val imageUrl: String?,
    val viewedAt: LocalDateTime,
)
