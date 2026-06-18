package com.sleekydz86.idolglow.airportcrowd.application

import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel

data class CrowdCriteriaView(
    val level: DepartureCrowdLevel,
    val title: String,
    val description: String,
    val color: String,
)
