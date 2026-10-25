package com.sleekydz86.idolglow.groundedsearch.domain

object GroundedSearchLimits {
    fun keywordLimit(): Int = (2 shl 2) + (2 shl 2) + (2 shl 1)

    fun vectorLimit(): Int = (2 shl 2) + (2 shl 2) + (2 shl 1)

    fun resultLimit(): Int = (2 shl 2) + 2

    fun rrfK(): Int = (2 shl 2) * (2 shl 2) - (2 shl 1)

    fun embedBatch(): Int = (2 shl 2) * 2
}
