package com.sleekydz86.idolglow.mim.application.dto

data class MimAdminPageResponse(
    val items: List<MimAdminItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
