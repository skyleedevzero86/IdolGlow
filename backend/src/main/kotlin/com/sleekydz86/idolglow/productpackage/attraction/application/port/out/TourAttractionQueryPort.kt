package com.sleekydz86.idolglow.productpackage.attraction.application.port.out

import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction

interface TourAttractionQueryPort {
    fun fetchAreaBasedAttractions(
        baseYm: String,
        areaCode: Int,
        signguCode: Int,
        size: Int,
    ): List<TourAttraction>
}
