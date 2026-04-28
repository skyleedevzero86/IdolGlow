package com.sleekydz86.idolglow.airportcrowd.application.port.out

import com.sleekydz86.idolglow.airportcrowd.domain.PassengerForecast

interface PassengerForecastQueryPort {
    /**
     * @param selectDate 0=오늘, 1=내일
     */
    fun fetch(selectDate: Int, pageNo: Int = 1, numOfRows: Int = 10000): List<PassengerForecast>
}
