package com.sleekydz86.idolglow.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@ActiveProfiles("test")
@Suppress("UtilityClassWithPublicConstructor")
abstract class PostgresIntegrationTest {
    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:17-alpine").apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "30" }
            registry.add("spring.datasource.hikari.connection-init-sql") { "SET lock_timeout TO '3s'" }
            registry.add("payment.toss.enabled") { "true" }
            registry.add("payment.toss.use-toss-provider") { "true" }
            registry.add("payment.toss.secret-key") { "test-toss-secret" }
            registry.add("payment.toss.client-key") { "test-client" }
            registry.add("spring.task.scheduling.enabled") { "false" }
        }
    }
}
