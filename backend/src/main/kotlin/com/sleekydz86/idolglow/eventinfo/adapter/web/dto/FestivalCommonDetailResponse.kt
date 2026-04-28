package com.sleekydz86.idolglow.eventinfo.adapter.web.dto

data class FestivalCommonDetailResponse(
    val contentId: String,
    val contentTypeId: String?,
    val title: String?,
    val homepage: String?,
    val overview: String?,
    val address: String?,
    val addressDetail: String?,
    val mapX: Double?,
    val mapY: Double?,
    val tel: String?,
    val firstImage: String?,
    val firstImage2: String?,
)
