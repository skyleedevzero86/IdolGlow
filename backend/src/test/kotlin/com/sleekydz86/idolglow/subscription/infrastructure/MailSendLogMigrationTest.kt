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

@DisplayName("메일 발송 기록 마이그레이션")
class MailSendLogMigrationTest {
    @Test
    @DisplayName("postgres 마이그레이션이 적용되고 mail_send_logs 와 인덱스가 생긴다")
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
                assertEquals(EXPECTED_COLUMNS, names(connection, COLUMNS_SQL))
                assertEquals(EXPECTED_INDEXES, names(connection, INDEXES_SQL))
                insertLog(connection, "NEWSLETTER", "SENT")
                assertEquals(CHECK_VIOLATION, failedState { insertLog(connection, "UNKNOWN", "SENT") })
                assertEquals(CHECK_VIOLATION, failedState { insertLog(connection, "NEWSLETTER", "BOUNCED") })
            }
        }
    }

    private fun failedState(action: () -> Unit): String? = assertThrows(SQLException::class.java) { action() }.sqlState

    private fun names(
        connection: Connection,
        sql: String,
    ): Set<String> =
        connection.prepareStatement(sql).executeQuery().use { rows ->
            buildSet {
                while (rows.next()) add(rows.getString(1))
            }
        }

    private fun insertLog(
        connection: Connection,
        kind: String,
        status: String,
    ) {
        connection
            .prepareStatement(
                """
                insert into mail_send_logs (mail_kind, recipient_email, subject, status, created_at)
                values (?, 'reader@idolglow.test', '제목', ?, now())
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, kind)
                statement.setString(2, status)
                statement.executeUpdate()
            }
    }

    companion object {
        private const val CHECK_VIOLATION = "23514"
        private const val COLUMNS_SQL =
            "select column_name from information_schema.columns where table_name = 'mail_send_logs'"
        private const val INDEXES_SQL =
            "select indexname from pg_indexes where tablename = 'mail_send_logs' and indexname like 'idx_%'"
        private val EXPECTED_COLUMNS =
            setOf(
                "id",
                "mail_kind",
                "recipient_email",
                "subject",
                "html_body",
                "text_body",
                "status",
                "error",
                "dispatch_id",
                "sent_at",
                "created_at",
            )
        private val EXPECTED_INDEXES =
            setOf(
                "idx_mail_send_logs_created",
                "idx_mail_send_logs_kind_created",
                "idx_mail_send_logs_dispatch",
            )
    }
}
