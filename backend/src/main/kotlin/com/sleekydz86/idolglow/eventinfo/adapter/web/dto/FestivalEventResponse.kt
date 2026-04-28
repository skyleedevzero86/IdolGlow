package com.sleekydz86.idolglow.eventinfo.adapter.web.dto

data class FestivalEventResponse(
    val contentId: String,
    val title: String,
    val address: String?,
    val eventStartDate: String?,
    val eventEndDate: String?,
    val thumbnailImageUrl: String?,
    val imageUrl: String?,
    val mapX: Double?,
    val mapY: Double?,
    val phone: String?,
    val detailUrl: String?,
    val category: String?,
    val synopsis: String?,
    val source: String,
    val cast: String? = null,
    val runningTime: String? = null,
    val age: String? = null,
    val bookingPlaces: String? = null,
    val introImageUrls: List<String> = emptyList(),
)
