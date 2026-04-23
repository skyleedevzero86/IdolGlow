package com.sleekydz86.idolglow.productpackage.attraction.domain

data class TourAttraction(
    val attractionCode: String,
    val name: String,
    val areaCode: Int,
    val areaName: String?,
    val signguCode: Int,
    val signguName: String?,
    val categoryLarge: String?,
    val categoryMiddle: String?,
    val rank: Int,
    val mapX: Double?,
    val mapY: Double?,
    val baseYm: String,
)
