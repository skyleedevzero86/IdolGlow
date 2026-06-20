package com.sleekydz86.idolglow.sitecontent.application.dto

data class SiteHeroSlideResponse(
    val imageId: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String,
    val linkUrl: String,
    val categoryLabel: String,
)
