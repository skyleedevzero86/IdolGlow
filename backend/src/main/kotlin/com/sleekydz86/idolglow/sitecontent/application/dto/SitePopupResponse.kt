package com.sleekydz86.idolglow.sitecontent.application.dto

data class SitePopupResponse(
    val popupId: String,
    val title: String,
    val imageUrl: String?,
    val linkUrl: String?,
    val linkTarget: String,
    val noticeStartDate: String?,
    val noticeEndDate: String?,
    val stopViewYn: String?,
)
