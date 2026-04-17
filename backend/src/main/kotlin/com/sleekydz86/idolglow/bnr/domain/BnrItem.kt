package com.sleekydz86.idolglow.bnr.domain

data class BnrItem(
    val bannerId: String,
    val domainId: String?,
    val bannerName: String?,
    val linkUrl: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val description: String?,
    val sortOrder: Int,
    val activeYn: String?,
    val createdBy: String?,
    val createdAtFormatted: String?,
    val domainName: String?,
)
