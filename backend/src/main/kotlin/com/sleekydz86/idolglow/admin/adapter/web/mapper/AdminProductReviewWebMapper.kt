package com.sleekydz86.idolglow.admin.adapter.web.mapper

import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminProductReviewPageResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminProductReviewSummaryResponse
import com.sleekydz86.idolglow.admin.application.dto.AdminProductReviewPageResult
import com.sleekydz86.idolglow.admin.application.dto.AdminProductReviewSummaryResult

fun AdminProductReviewPageResult.toWebResponse(): AdminProductReviewPageResponse =
    AdminProductReviewPageResponse(
        reviews = reviews.map { it.toWebResponse() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
        hasNext = hasNext,
    )

fun AdminProductReviewSummaryResult.toWebResponse(): AdminProductReviewSummaryResponse =
    AdminProductReviewSummaryResponse(
        reviewId = reviewId,
        productId = productId,
        productName = productName,
        userId = userId,
        rating = rating,
        content = content,
        createdAt = createdAt,
        hidden = hidden,
        hiddenReason = hiddenReason,
        helpfulCount = helpfulCount,
        images = images,
    )
