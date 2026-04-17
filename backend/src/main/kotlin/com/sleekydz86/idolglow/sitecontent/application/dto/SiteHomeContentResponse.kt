package com.sleekydz86.idolglow.sitecontent.application.dto

data class SiteHomeContentResponse(
    val heroSlides: List<SiteHeroSlideResponse>,
    val banners: List<SiteBannerResponse>,
    val popups: List<SitePopupResponse>,
)

data class SiteHeroSlideResponse(
    val imageId: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String,
    val linkUrl: String,
    val categoryLabel: String,
)

data class SiteBannerResponse(
    val bannerId: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val linkUrl: String,
)

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
