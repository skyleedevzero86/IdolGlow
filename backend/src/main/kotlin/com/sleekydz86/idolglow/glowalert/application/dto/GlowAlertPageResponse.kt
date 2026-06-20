package com.sleekydz86.idolglow.glowalert.application.dto

data class GlowAlertPageResponse(
    val items: List<GlowAlertItemResponse>,
    val categories: List<GlowAlertCategoryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val tab: String,
    val activeCategory: String,
)
