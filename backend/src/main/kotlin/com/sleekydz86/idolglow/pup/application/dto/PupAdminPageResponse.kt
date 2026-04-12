package com.sleekydz86.idolglow.pup.application.dto

data class PupAdminPageResponse(
    val items: List<PupAdminItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
