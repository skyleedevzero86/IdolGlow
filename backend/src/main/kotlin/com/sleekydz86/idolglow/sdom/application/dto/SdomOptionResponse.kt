package com.sleekydz86.idolglow.sdom.application.dto

data class SdomOptionResponse(
    val domainId: String,
    val domainName: String?,
    val domainPath: String?,
    val description: String?,
    val useYn: String?,
    val sortOrder: Int,
)
