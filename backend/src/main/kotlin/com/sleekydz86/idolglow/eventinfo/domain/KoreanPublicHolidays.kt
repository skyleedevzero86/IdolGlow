package com.sleekydz86.idolglow.eventinfo.domain

import java.time.LocalDate

object KoreanPublicHolidays {
    private val DATES: Set<LocalDate> =
        listOf(
            "2026-12-25",
            "2027-01-01",
            "2027-02-06",
            "2027-02-07",
            "2027-02-08",
            "2027-02-09",
            "2027-03-01",
            "2027-05-01",
            "2027-05-03",
            "2027-05-05",
            "2027-05-13",
            "2027-06-06",
            "2027-07-17",
            "2027-07-19",
            "2027-08-15",
            "2027-08-16",
            "2027-09-14",
            "2027-09-15",
            "2027-09-16",
            "2027-10-03",
            "2027-10-04",
            "2027-10-09",
            "2027-10-11",
            "2027-12-25",
            "2027-12-27",
        ).map(LocalDate::parse).toSet()

    fun contains(date: LocalDate): Boolean = date in DATES
}
