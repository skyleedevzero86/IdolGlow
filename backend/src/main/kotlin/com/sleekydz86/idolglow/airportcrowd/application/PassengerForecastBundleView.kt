package com.sleekydz86.idolglow.airportcrowd.application

data class PassengerForecastBundleView(
    val today: List<PassengerForecastView>,
    val tomorrow: List<PassengerForecastView>,
)
