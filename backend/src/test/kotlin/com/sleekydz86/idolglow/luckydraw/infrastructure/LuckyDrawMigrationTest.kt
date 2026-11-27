package com.sleekydz86.idolglow.luckydraw.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp

@DisplayName("응모 행사 테이블 마이그레이션")
class LuckyDrawMigrationTest {
    @DisplayName("PostgreSQL 마이그레이션은 올바른 행을 받고 제약을 어기는 행은 한 가지씩 모두 막는다")
    @Test
    fun postgresConstraints() {
        // given
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")

            // when
            connect(container).use { connection ->
                val drawId = insertDraw(connection, VALID)
                insertEntry(connection, drawId, "2026-12-01")
                insertEntry(connection, drawId, "2026-12-02")
                insertWinner(connection, drawId, 10L, 1)
                val failures = violations(connection, drawId)

                // then
                assertEquals(VIOLATION_COUNT, failures.size)
                failures.forEach { state ->
                    assertEquals(true, state == CHECK_STATE || state == UNIQUE_STATE, "sqlState=$state")
                }
                assertEquals(2, count(connection, "lucky_draw_entries"))
            }
        }
    }

    @DisplayName("MySQL 마이그레이션은 올바른 행을 받고 제약을 어기는 행은 한 가지씩 모두 막는다")
    @Test
    fun mysqlConstraints() {
        // given
        MySQLContainer("mysql:8.4").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")

            // when
            connect(container).use { connection ->
                val drawId = insertDraw(connection, VALID)
                insertEntry(connection, drawId, "2026-12-01")
                insertEntry(connection, drawId, "2026-12-02")
                insertWinner(connection, drawId, 10L, 1)
                val failures = violations(connection, drawId)

                // then
                assertEquals(VIOLATION_COUNT, failures.size)
                assertEquals(2, count(connection, "lucky_draw_entries"))
            }
        }
    }

    private fun violations(
        connection: Connection,
        drawId: Long,
    ): List<String?> =
        listOf(
            { insertDraw(connection, VALID.copy(winnerCount = 0)) },
            { insertDraw(connection, VALID.copy(winnerCount = 11)) },
            { insertDraw(connection, VALID.copy(endsAt = VALID.startsAt)) },
            { insertDraw(connection, VALID.copy(drawAt = "2026-12-08 10:59:59")) },
            { insertDraw(connection, VALID.copy(seed = 1L)) },
            { insertEntry(connection, drawId, "2026-12-01") },
            { insertWinner(connection, drawId, 10L, 2) },
            { insertWinner(connection, drawId, 11L, 1) },
            { insertTicket(connection, drawId, -1) },
        ).map { action -> assertThrows(SQLException::class.java) { action() }.sqlState }

    private fun migrate(
        container: JdbcDatabaseContainer<*>,
        location: String,
        prefix: String,
    ) {
        Flyway
            .configure()
            .dataSource(container.jdbcUrl, container.username, container.password)
            .locations(location)
            .sqlMigrationPrefix(prefix)
            .load()
            .migrate()
    }

    private fun connect(container: JdbcDatabaseContainer<*>): Connection =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password)

    private fun insertDraw(
        connection: Connection,
        row: DrawRow,
    ): Long =
        connection
            .prepareStatement(
                """
                insert into lucky_draws (title, prize_name, prize_description, winner_count,
                    entry_starts_at, entry_ends_at,
                    draw_at, draw_seed, drawn_at, created_at, updated_at)
                values ('응모', '경품', '설명', ?, ?, ?, ?, ?, null,
                    current_timestamp, current_timestamp)
                """.trimIndent(),
                arrayOf("id"),
            ).use { statement ->
                statement.setInt(1, row.winnerCount)
                statement.setTimestamp(2, Timestamp.valueOf(row.startsAt))
                statement.setTimestamp(3, Timestamp.valueOf(row.endsAt))
                statement.setTimestamp(4, Timestamp.valueOf(row.drawAt))
                statement.setObject(5, row.seed)
                statement.executeUpdate()
                statement.generatedKeys.use { keys ->
                    keys.next()
                    keys.getLong(1)
                }
            }

    private fun insertEntry(
        connection: Connection,
        drawId: Long,
        date: String,
    ) {
        execute(
            connection,
            "insert into lucky_draw_entries (lucky_draw_id, user_id, entry_date, entered_at, created_at, updated_at) " +
                "values ($drawId, 10, '$date', current_timestamp, current_timestamp, current_timestamp)",
        )
    }

    private fun insertWinner(
        connection: Connection,
        drawId: Long,
        userId: Long,
        order: Int,
    ) {
        execute(
            connection,
            "insert into lucky_draw_winners (lucky_draw_id, user_id, winner_order, created_at, updated_at) " +
                "values ($drawId, $userId, $order, current_timestamp, current_timestamp)",
        )
    }

    private fun insertTicket(
        connection: Connection,
        drawId: Long,
        entryCount: Int,
    ) {
        execute(
            connection,
            "insert into lucky_draw_tickets " +
                "(lucky_draw_id, user_id, entry_count, purchase_count, tickets, created_at, updated_at) " +
                "values ($drawId, 10, $entryCount, 0, 0, current_timestamp, current_timestamp)",
        )
    }

    private fun execute(
        connection: Connection,
        sql: String,
    ) {
        connection.createStatement().use { it.executeUpdate(sql) }
    }

    private fun count(
        connection: Connection,
        table: String,
    ): Int =
        connection.createStatement().use { statement ->
            statement.executeQuery("select count(*) from $table").use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }

    private data class DrawRow(
        val winnerCount: Int,
        val startsAt: String,
        val endsAt: String,
        val drawAt: String,
        val seed: Long?,
    )

    companion object {
        private const val CHECK_STATE = "23514"
        private const val UNIQUE_STATE = "23505"
        private const val VIOLATION_COUNT = 9
        private val VALID = DrawRow(1, "2026-12-01 11:00:00", "2026-12-08 11:00:00", "2026-12-08 13:00:00", null)
    }
}
