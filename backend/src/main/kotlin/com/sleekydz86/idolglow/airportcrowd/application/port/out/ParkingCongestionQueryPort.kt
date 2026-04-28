package com.sleekydz86.idolglow.airportcrowd.application.port.out

import com.sleekydz86.idolglow.airportcrowd.domain.ParkingCongestion

interface ParkingCongestionQueryPort {
    fun fetchCurrent(
        pageNo: Int = 1,
        numOfRows: Int = 1000,
    ): List<ParkingCongestion>
}
