package com.sleekydz86.idolglow

import com.sleekydz86.idolglow.global.config.AppCalendarProperties
import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.config.GemmaProperties
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import com.sleekydz86.idolglow.global.config.TossPaymentProperties
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
)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
