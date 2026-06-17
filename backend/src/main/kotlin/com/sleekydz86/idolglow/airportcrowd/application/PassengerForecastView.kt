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

data class PassengerForecastView(
    val date: String?,
    val timeSlot: String,
    val terminal1DepartureTotal: Int?,
    val terminal2DepartureTotal: Int?,
    val terminal1ArrivalTotal: Int?,
    val terminal2ArrivalTotal: Int?,
    val totalDeparture: Int,
    val level: DepartureCrowdLevel,
)
