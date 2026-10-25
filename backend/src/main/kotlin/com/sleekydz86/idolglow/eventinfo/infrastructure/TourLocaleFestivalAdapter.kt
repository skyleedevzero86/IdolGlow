package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleFestivalPort
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import org.springframework.stereotype.Component

@Component
class TourLocaleFestivalAdapter(
    private val tourFestivalApiClient: TourFestivalApiClient,
) : TourLocaleFestivalPort {
    override fun searchFestival(
        locale: String,
        eventStartDate: String,
        eventEndDate: String,
        pageNo: Int,
        numOfRows: Int,
    ): List<FestivalEvent> =
        tourFestivalApiClient.searchFestivalInLocale(
            locale = locale,
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
            pageNo = pageNo,
            numOfRows = numOfRows,
        )
}
