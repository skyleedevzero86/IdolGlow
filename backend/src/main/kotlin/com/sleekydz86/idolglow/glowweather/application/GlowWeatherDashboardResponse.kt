package com.sleekydz86.idolglow.glowweather.application

data class GlowWeatherDashboardResponse(
    val selectedRegionId: String,
    val regions: List<GlowWeatherRegionSummary>,
    val region: GlowWeatherSelectedRegion,
    val current: CurrentWeatherResponse,
    val monthlySummary: GlowWeatherMonthlySummary,
    val outlookSummary: String,
    val forecast: List<GlowWeatherForecastDay>,
    val recommendations: List<GlowWeatherRecommendation>,
    val windGuide: GlowWeatherWindGuide,
    val forecastFromApi: Boolean,
    val currentFromApi: Boolean,
    val generatedAt: String,
)
