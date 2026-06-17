package com.sleekydz86.idolglow.sitecontent.application.dto

data class SiteHomeContentResponse(
    val heroSlides: List<SiteHeroSlideResponse>,
    val banners: List<SiteBannerResponse>,
    val popups: List<SitePopupResponse>,
)
