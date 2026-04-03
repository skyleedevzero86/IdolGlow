package com.sleekydz86.idolglow

import com.sleekydz86.idolglow.global.config.AppCalendarProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.config.GemmaProperties
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    MinioStorageProperties::class,
    AppPublicUrlProperties::class,
    AppCalendarProperties::class,
    TossPaymentProperties::class,
    GemmaProperties::class,
)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
