package com.sleekydz86.idolglow.bnr.domain

data class BnrListCriteria(
    val pageIndex: Int,
    val pageSize: Int,
    val domainId: String,
    val searchType: String,
    val keyword: String,
)
