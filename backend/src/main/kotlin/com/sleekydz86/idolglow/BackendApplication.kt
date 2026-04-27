package com.sleekydz86.idolglow

import com.sleekydz86.idolglow.global.infrastructure.config.AppCalendarProperties
import com.sleekydz86.idolglow.global.infrastructure.config.AppMailProperties
import com.sleekydz86.idolglow.global.infrastructure.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.infrastructure.config.GemmaProperties
import com.sleekydz86.idolglow.global.infrastructure.config.MinioStorageProperties
import com.sleekydz86.idolglow.global.infrastructure.config.ExchangeAirportHubProperties
import com.sleekydz86.idolglow.global.infrastructure.config.KoreaEximExchangeProperties
import com.sleekydz86.idolglow.global.infrastructure.config.KopisApiProperties
import com.sleekydz86.idolglow.global.infrastructure.config.NaverDirectionsProperties
import com.sleekydz86.idolglow.global.infrastructure.config.SubwayProperties
import com.sleekydz86.idolglow.global.infrastructure.config.SeoulSjwApiProperties
import com.sleekydz86.idolglow.global.infrastructure.config.SpcdeInfoApiProperties
import com.sleekydz86.idolglow.global.infrastructure.config.TourApiProperties
import com.sleekydz86.idolglow.global.infrastructure.config.TourKorApiProperties
import com.sleekydz86.idolglow.global.infrastructure.config.TossPaymentProperties
import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(
    MinioStorageProperties::class,
    AppPublicUrlProperties::class,
    AppCalendarProperties::class,
    AppMailProperties::class,
    TossPaymentProperties::class,
    GemmaProperties::class,
    PlatformAuthProperties::class,
    TourApiProperties::class,
    TourKorApiProperties::class,
    SeoulSjwApiProperties::class,
    SpcdeInfoApiProperties::class,
    KoreaEximExchangeProperties::class,
    KopisApiProperties::class,
    NaverDirectionsProperties::class,
    ExchangeAirportHubProperties::class,
    SubwayProperties::class,
)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
