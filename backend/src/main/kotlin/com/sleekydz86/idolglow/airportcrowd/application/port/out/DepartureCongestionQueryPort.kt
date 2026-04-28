package com.sleekydz86.idolglow.airportcrowd.application.port.out

import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCongestion

interface DepartureCongestionQueryPort {
    fun fetchCurrent(
        terminalId: String?,
        gateId: String?,
        pageNo: Int = 1,
        numOfRows: Int = 1000,
    ): List<DepartureCongestion>
}
