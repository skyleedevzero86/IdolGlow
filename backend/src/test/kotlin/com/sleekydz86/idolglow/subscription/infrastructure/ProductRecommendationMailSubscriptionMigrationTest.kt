package com.sleekydz86.idolglow.subscription.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@DisplayName("상품 추천 메일 구독 마이그레이션")
class ProductRecommendationMailSubscriptionMigrationTest {
    @Test
    @DisplayName("postgres 마이그레이션이 적용되고 product_recommendation_mail_subscriptions 가 생긴다")
    fun postgresMigrationCreatesTable() {
        // given
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()

            // when
            Flyway
                .configure()
                .dataSource(container.jdbcUrl, container.username, container.password)
                .locations("classpath:db/migration/postgres")
                .sqlMigrationPrefix("ps")
                .load()
                .migrate()

            // then
            DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
                assertEquals(EXPECTED_COLUMNS, columns(connection))
                val userId = insertUser(connection, "reco1")
                insertSubscription(connection, userId, "AUTO")
                assertEquals(UNIQUE_VIOLATION, failedState { insertSubscription(connection, userId, "AUTO") })
                val otherUserId = insertUser(connection, "reco2")
                assertEquals(CHECK_VIOLATION, failedState { insertSubscription(connection, otherUserId, "WEEKLY") })
            }
        }
    }

    private fun failedState(action: () -> Unit): String? = assertThrows(SQLException::class.java) { action() }.sqlState

    private fun columns(connection: Connection): Set<String> =
        connection
            .prepareStatement(
                """
                select column_name from information_schema.columns
                where table_name = 'product_recommendation_mail_subscriptions'
                """.trimIndent(),
            ).executeQuery()
            .use { rows ->
                buildSet {
                    while (rows.next()) add(rows.getString(1))
                }
            }

    private fun insertUser(
        connection: Connection,
        nickname: String,
    ): Long =
        connection
            .prepareStatement(
                """
                insert into users (email, nickname, role)
                values (concat(?, '@example.com'), ?, 'USER')
                returning id
                """.trimIndent(),
            ).apply {
                setString(1, nickname)
                setString(2, nickname)
            }.executeQuery()
            .use { rows ->
                rows.next()
                rows.getLong(1)
            }

    private fun insertSubscription(
        connection: Connection,
        userId: Long,
        preferredBasis: String,
    ) {
        connection
            .prepareStatement(
                """
                insert into product_recommendation_mail_subscriptions
                    (user_id, active, preferred_basis, consented_at, consent_source, created_at, updated_at)
                values (?, true, ?, now(), 'MY_SURVEY', now(), now())
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, preferredBasis)
                statement.executeUpdate()
            }
    }

    companion object {
        private const val UNIQUE_VIOLATION = "23505"
        private const val CHECK_VIOLATION = "23514"
        private val EXPECTED_COLUMNS =
            setOf(
                "id",
                "user_id",
                "active",
                "preferred_basis",
                "consented_at",
                "consent_source",
                "token_version",
                "unsubscribed_at",
                "last_sent_at",
                "last_sent_product_ids",
                "consent_reminded_at",
                "created_at",
                "updated_at",
            )
    }
}
