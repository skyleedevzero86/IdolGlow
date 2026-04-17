package com.sleekydz86.idolglow.bnr.application.dto

data class BnrAdminPageResponse(
    val items: List<BnrAdminItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
