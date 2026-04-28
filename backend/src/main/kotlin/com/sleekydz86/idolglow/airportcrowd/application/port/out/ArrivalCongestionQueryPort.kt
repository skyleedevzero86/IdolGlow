package com.sleekydz86.idolglow.airportcrowd.application.port.out

import com.sleekydz86.idolglow.airportcrowd.domain.ArrivalCongestion

interface ArrivalCongestionQueryPort {
    fun fetchCurrent(
        terminal: String?,
        airport: String?,
        pageNo: Int = 1,
        numOfRows: Int = 1000,
    ): List<ArrivalCongestion>
}
