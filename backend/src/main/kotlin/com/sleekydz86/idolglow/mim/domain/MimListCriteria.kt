package com.sleekydz86.idolglow.mim.domain

data class MimListCriteria(
    val pageIndex: Int,
    val pageSize: Int,
    val domainId: String,
    val searchType: String,
    val keyword: String,
)
