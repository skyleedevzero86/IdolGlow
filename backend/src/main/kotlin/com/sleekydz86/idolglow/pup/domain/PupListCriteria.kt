package com.sleekydz86.idolglow.pup.domain

data class PupListCriteria(
    val pageIndex: Int,
    val pageSize: Int,
    val domainId: String,
    val searchType: String,
    val keyword: String,
)
