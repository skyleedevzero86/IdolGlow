package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment

@Configuration
class FlywayLocalhostChecksumRelaxationConfig(
    private val environment: Environment,
) {
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun flywayValidateOnMigrateCustomizer(): FlywayConfigurationCustomizer =
        FlywayConfigurationCustomizer { configuration ->
            val strict =
                environment.getProperty("FLYWAY_STRICT_VALIDATE", Boolean::class.javaObjectType, false) == true
            if (strict) {
                return@FlywayConfigurationCustomizer
            }
            val url = environment.getProperty("spring.datasource.url", "")
            val localDb =
                url.contains("localhost", ignoreCase = true) ||
                    url.contains("127.0.0.1", ignoreCase = true)
            if (localDb) {
                configuration.validateOnMigrate(false)
            }
        }
}
