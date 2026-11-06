package com.sleekydz86.idolglow.subway.infrastructure.location

import com.sleekydz86.idolglow.eventinfo.infrastructure.TourFestivalApiClient
import com.sleekydz86.idolglow.subway.application.port.out.SubwayNearbyPlacePort
import com.sleekydz86.idolglow.subway.domain.SubwayNearbyPlace
import com.sleekydz86.idolglow.subway.domain.SubwayStationLocation
import org.springframework.stereotype.Component

@Component
class TourSubwayNearbyPlaceAdapter(
    private val tourApiClient: TourFestivalApiClient,
) : SubwayNearbyPlacePort {
    override fun nearby(location: SubwayStationLocation): List<SubwayNearbyPlace> =
        tourApiClient
            .locationBasedList(
                mapX = location.longitude,
                mapY = location.latitude,
                radiusMeters = RADIUS_METERS,
                numOfRows = FETCH_ROWS,
            ).mapNotNull { place ->
                CATEGORIES[place.contentTypeId]?.let {
                    SubwayNearbyPlace(name = place.title, category = it, distanceMeters = place.distanceMeters)
                }
            }.distinctBy { it.name }
            .take(MAX_PLACES)

    companion object {
        private const val RADIUS_METERS = 1000
        private const val FETCH_ROWS = 60
        private const val MAX_PLACES = 5
        private val CATEGORIES =
            mapOf(
                "12" to "관광지",
                "14" to "문화시설",
                "15" to "축제·공연",
                "28" to "레포츠",
                "38" to "쇼핑",
            )
    }
}
