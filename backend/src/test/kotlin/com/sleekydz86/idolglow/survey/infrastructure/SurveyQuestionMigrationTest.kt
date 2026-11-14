package com.sleekydz86.idolglow.survey.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager

@DisplayName("설문 문항 추천 신호 마이그레이션")
class SurveyQuestionMigrationTest {
    @Test
    @DisplayName("postgres 마이그레이션이 적용되고 survey_question.recommendation_signal 이 생긴다")
    fun postgresMigrationAddsRecommendationSignal() {
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()

            val applied = migrate(container, "classpath:db/migration/postgres", "ps")

            assertTrue(applied > 0)
            assertEquals("character varying", columnType(container))
        }
    }

    @Test
    @DisplayName("mysql 마이그레이션이 적용되고 survey_question.recommendation_signal 이 생긴다")
    fun mysqlMigrationAddsRecommendationSignal() {
        MySQLContainer("mysql:8.4").use { container ->
            container.start()

            val applied = migrate(container, "classpath:db/migration/mysql", "ms")

            assertTrue(applied > 0)
            assertEquals("varchar", columnType(container))
        }
    }

    private fun migrate(
        container: JdbcDatabaseContainer<*>,
        location: String,
        prefix: String,
    ): Int =
        Flyway
            .configure()
            .dataSource(container.jdbcUrl, container.username, container.password)
            .locations(location)
            .sqlMigrationPrefix(prefix)
            .load()
            .migrate()
            .migrationsExecuted

    private fun columnType(container: JdbcDatabaseContainer<*>): String? =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
            connection
                .prepareStatement(
                    """
                    select data_type from information_schema.columns
                    where lower(table_name) = 'survey_question' and lower(column_name) = 'recommendation_signal'
                    """.trimIndent(),
                ).executeQuery()
                .use { rows -> if (rows.next()) rows.getString(1).lowercase() else null }
        }
}
