package com.sleekydz86.idolglow.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.mysql.MySQLContainer

@SpringBootTest
@ActiveProfiles("test")
@Suppress("UtilityClassWithPublicConstructor")
abstract class MysqlIntegrationTest {
    companion object {
        @JvmStatic
        val mysql: MySQLContainer =
            MySQLContainer("mysql:8.4")
                .withCommand("--innodb-lock-wait-timeout=10")
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "${mysql.jdbcUrl}?serverTimezone=Asia/Seoul" }
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.flyway.locations") { "classpath:db/migration/mysql" }
            registry.add("spring.flyway.sql-migration-prefix") { "ms" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQLDialect" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "30" }
            registry.add("spring.task.scheduling.enabled") { "false" }
        }
    }
}
