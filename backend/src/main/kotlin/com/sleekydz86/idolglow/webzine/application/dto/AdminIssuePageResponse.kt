package com.sleekydz86.idolglow.webzine.application.dto

data class AdminIssuePageResponse(
    val issues: List<AdminIssueSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val latestVolume: Int,
    val totalArticleCount: Int,
    val availableYears: List<Int>,
    val availableMonths: List<Int>,
    val availableVolumes: List<Int>,
)
