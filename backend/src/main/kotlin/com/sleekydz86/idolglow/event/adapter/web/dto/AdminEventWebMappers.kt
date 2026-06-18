package com.sleekydz86.idolglow.event.adapter.web.dto

import com.sleekydz86.idolglow.event.application.dto.AdminEventDetailResult
import com.sleekydz86.idolglow.event.application.dto.AdminEventPageResult
import com.sleekydz86.idolglow.event.application.dto.AdminEventSummaryResult

fun AdminEventPageResult.toWebResponse(): AdminEventPageResponse =
    AdminEventPageResponse(
        items = items.map { it.toWebResponse() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

fun AdminEventSummaryResult.toWebResponse(): AdminEventSummaryResponse =
    AdminEventSummaryResponse(
        documentId = documentId,
        title = title,
        author = author,
        introduction = introduction,
        thumbnailImageUrl = thumbnailImageUrl,
        tags = tags,
        status = status,
        updatedAt = updatedAt,
        viewCount = viewCount,
    )

fun AdminEventDetailResult.toWebResponse(): AdminEventDetailResponse =
    AdminEventDetailResponse(
        documentId = documentId,
        title = title,
        author = author,
        markdown = markdown,
        tags = tags,
        urlSlug = urlSlug,
        introduction = introduction,
        thumbnailImageUrl = thumbnailImageUrl,
        status = status,
        updatedAt = updatedAt,
        viewCount = viewCount,
    )
