package com.sleekydz86.idolglow.sitecontent.application.dto

data class SiteBannerResponse(
    val bannerId: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val linkUrl: String,
)
