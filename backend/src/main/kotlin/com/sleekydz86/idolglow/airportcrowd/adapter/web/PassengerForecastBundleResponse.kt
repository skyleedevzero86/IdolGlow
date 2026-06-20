package com.sleekydz86.idolglow.airportcrowd.adapter.web

import com.sleekydz86.idolglow.airportcrowd.application.PassengerForecastBundleView

data class PassengerForecastBundleResponse(
    val today: List<PassengerForecastResponse>,
    val tomorrow: List<PassengerForecastResponse>,
) {
    companion object {
        fun from(view: PassengerForecastBundleView): PassengerForecastBundleResponse =
            PassengerForecastBundleResponse(
                today = view.today.map(PassengerForecastResponse::from),
                tomorrow = view.tomorrow.map(PassengerForecastResponse::from),
            )
    }
}
