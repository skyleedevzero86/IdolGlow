package com.sleekydz86.idolglow

import com.sleekydz86.idolglow.global.config.AppCalendarProperties
import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.config.CultureInfoApiProperties
import com.sleekydz86.idolglow.global.config.ExchangeAirportHubProperties
import com.sleekydz86.idolglow.global.config.GemmaProperties
import com.sleekydz86.idolglow.global.config.IncheonAirportArrivalsCongestionProperties
import com.sleekydz86.idolglow.global.config.IncheonAirportCongestionProperties
import com.sleekydz86.idolglow.global.config.IncheonAirportParkingProperties
import com.sleekydz86.idolglow.global.config.IncheonAirportPassengerForecastProperties
import com.sleekydz86.idolglow.global.config.KmaWeatherProperties
import com.sleekydz86.idolglow.global.config.KopisApiProperties
import com.sleekydz86.idolglow.global.config.KoreaEximExchangeProperties
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import com.sleekydz86.idolglow.global.config.NaverDirectionsProperties
import com.sleekydz86.idolglow.global.config.SeoulSjwApiProperties
import com.sleekydz86.idolglow.global.config.SpcdeInfoApiProperties
import com.sleekydz86.idolglow.global.config.SubwayProperties
import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.global.config.TourApiProperties
import com.sleekydz86.idolglow.global.config.TourKorApiProperties
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
    CultureInfoApiProperties::class,
    KmaWeatherProperties::class,
    KoreaEximExchangeProperties::class,
    KopisApiProperties::class,
    NaverDirectionsProperties::class,
    ExchangeAirportHubProperties::class,
    SubwayProperties::class,
    IncheonAirportCongestionProperties::class,
    IncheonAirportArrivalsCongestionProperties::class,
    IncheonAirportPassengerForecastProperties::class,
    IncheonAirportParkingProperties::class,
)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
