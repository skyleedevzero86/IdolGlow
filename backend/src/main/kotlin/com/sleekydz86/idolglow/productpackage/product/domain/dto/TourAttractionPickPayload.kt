package com.sleekydz86.idolglow.productpackage.product.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TourAttractionPickPayload(
    val attractionCode: String = "",
    val name: String = "",
    val areaName: String? = null,
    val signguName: String? = null,
    val categoryLarge: String? = null,
    val categoryMiddle: String? = null,
    val rank: Int = 0,
    val mapX: Double? = null,
    val mapY: Double? = null,
    val score: Int = 0,
    val reason: String = "",
)
