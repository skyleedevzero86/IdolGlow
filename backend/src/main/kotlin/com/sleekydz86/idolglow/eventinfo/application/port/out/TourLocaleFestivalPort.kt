package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent

fun interface TourLocaleFestivalPort {
    fun searchFestival(
        locale: String,
        eventStartDate: String,
        eventEndDate: String,
        pageNo: Int,
        numOfRows: Int,
    ): List<FestivalEvent>
}
