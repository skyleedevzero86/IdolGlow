package com.sleekydz86.idolglow.subway.application.port.out

import com.sleekydz86.idolglow.subway.domain.SubwayLine

fun interface SubwayLineCatalogPort {
    fun loadAllLines(): List<SubwayLine>
}
