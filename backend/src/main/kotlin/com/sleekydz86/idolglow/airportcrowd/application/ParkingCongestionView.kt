package com.sleekydz86.idolglow.airportcrowd.application

import com.sleekydz86.idolglow.airportcrowd.application.port.out.ArrivalCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.application.port.out.DepartureCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.application.port.out.ParkingCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.application.port.out.PassengerForecastQueryPort
import com.sleekydz86.idolglow.airportcrowd.domain.ArrivalCongestion
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCongestion
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel
import com.sleekydz86.idolglow.airportcrowd.domain.PassengerForecast
import com.sleekydz86.idolglow.airportcrowd.domain.ParkingCongestion
import org.springframework.stereotype.Service

data class ParkingCongestionView(
    val terminal: String?,
    val floor: String,
    val parking: Int?,
    val parkingArea: Int?,
    val available: Int?,
    val occupancyRate: Double?,
    val observedAt: java.time.LocalDateTime?,
    val level: DepartureCrowdLevel,
)
