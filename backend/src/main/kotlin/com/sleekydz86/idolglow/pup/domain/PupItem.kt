package com.sleekydz86.idolglow.pup.domain

data class PupItem(
    val popupId: String,
    val domainId: String?,
    val title: String?,
    val fileUrl: String?,
    val linkTarget: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val noticeStartDate: String?,
    val noticeEndDate: String?,
    val stopViewYn: String?,
    val noticeYn: String?,
    val createdBy: String?,
    val createdAtFormatted: String?,
    val updatedBy: String?,
    val updatedAtFormatted: String?,
    val domainName: String?,
)
