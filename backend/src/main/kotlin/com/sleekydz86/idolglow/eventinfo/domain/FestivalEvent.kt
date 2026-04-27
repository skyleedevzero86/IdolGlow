package com.sleekydz86.idolglow.eventinfo.domain

data class FestivalEvent(
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
    val detailUrl: String? = null,
    val category: String? = null,
    val synopsis: String? = null,
    val source: String = "TOUR_API",
    val cast: String? = null,
    val runningTime: String? = null,
    val age: String? = null,
    val bookingPlaces: String? = null,
    val introImageUrls: List<String> = emptyList(),
)
