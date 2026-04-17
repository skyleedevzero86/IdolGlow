package com.sleekydz86.idolglow.mim.domain

data class MimItem(
    val imageId: String,
    val domainId: String?,
    val imageName: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val description: String?,
    val activeYn: String?,
    val createdBy: String?,
    val createdAtFormatted: String?,
    val domainName: String?,
)
