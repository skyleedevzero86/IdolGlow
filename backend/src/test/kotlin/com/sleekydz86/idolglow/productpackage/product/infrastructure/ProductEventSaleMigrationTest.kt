package com.sleekydz86.idolglow.productpackage.product.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@DisplayName("상품 이벤트 칸 마이그레이션")
class ProductEventSaleMigrationTest {
    @DisplayName("PostgreSQL 마이그레이션은 올바른 이벤트 값을 저장·조회하고 잘못된 조합은 한 가지씩 모두 막는다")
    @Test
    fun postgresMigrationGuardsEventShape() {
        // given
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")

            // when
            connect(container).use { connection ->
                val failures = INVALID_ROWS.map { row -> failure(connection, row) }

                // then
                assertValidRowsRoundTrip(connection)
                failures.forEach { error -> assertEquals("23514", error.sqlState) }
            }
        }
    }

    @DisplayName("MySQL 마이그레이션은 올바른 이벤트 값을 저장·조회하고 잘못된 조합은 한 가지씩 모두 막는다")
    @Test
    fun mysqlMigrationGuardsEventShape() {
        // given
        MySQLContainer("mysql:8.4").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")

            // when
            connect(container).use { connection ->
                val failures = INVALID_ROWS.map { row -> failure(connection, row) }

                // then
                assertValidRowsRoundTrip(connection)
                failures.forEach { error -> assertEquals(MYSQL_CHECK_VIOLATION, error.errorCode) }
            }
        }
    }

    private fun assertValidRowsRoundTrip(connection: Connection) {
        insert(connection, EventRow(null, null, null, null, null))
        val rateId = insert(connection, EventRow("2026-12-01", "2026-12-01", "DISCOUNT_RATE", 1, null))
        val fixedId = insert(connection, EventRow("2026-12-01", "2026-12-31", "FIXED_BASE_PRICE", null, "0"))
        assertEquals(EventRow("2026-12-01", "2026-12-01", "DISCOUNT_RATE", 1, null), read(connection, rateId))
        assertEquals(EventRow("2026-12-01", "2026-12-31", "FIXED_BASE_PRICE", null, "0.00"), read(connection, fixedId))
    }

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

    private fun failure(
        connection: Connection,
        row: EventRow,
    ): SQLException = assertThrows(SQLException::class.java) { insert(connection, row) }

    private fun insert(
        connection: Connection,
        row: EventRow,
    ): Long {
        connection
            .prepareStatement(
                """
                insert into products (name, description, created_at, updated_at, base_price,
                    event_starts_on, event_ends_on, event_price_type, event_discount_rate, event_base_price)
                values ('상품', '설명', current_timestamp, current_timestamp, 10000,
                    cast(? as date), cast(? as date), ?, ?, ?)
                """.trimIndent(),
                arrayOf("id"),
            ).use { statement ->
                statement.setString(1, row.startsOn)
                statement.setString(2, row.endsOn)
                statement.setString(3, row.priceType)
                statement.setObject(4, row.discountRate)
                statement.setBigDecimal(5, row.eventBasePrice?.let(::BigDecimal))
                statement.executeUpdate()
                statement.generatedKeys.use { keys ->
                    keys.next()
                    return keys.getLong(1)
                }
            }
    }

    private fun read(
        connection: Connection,
        id: Long,
    ): EventRow =
        connection
            .prepareStatement(
                """
                select event_starts_on, event_ends_on, event_price_type, event_discount_rate, event_base_price
                from products where id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { rows ->
                    rows.next()
                    EventRow(
                        rows.getString(1),
                        rows.getString(2),
                        rows.getString(3),
                        rows.getObject(4)?.let { (it as Number).toInt() },
                        rows.getBigDecimal(5)?.toPlainString(),
                    )
                }
            }

    private data class EventRow(
        val startsOn: String?,
        val endsOn: String?,
        val priceType: String?,
        val discountRate: Int?,
        val eventBasePrice: String?,
    )

    companion object {
        private const val MYSQL_CHECK_VIOLATION = 3819

        private val INVALID_ROWS =
            listOf(
                EventRow("2026-12-02", "2026-12-01", "DISCOUNT_RATE", 30, null),
                EventRow("2026-12-01", "2026-12-31", "DISCOUNT_RATE", 0, null),
                EventRow("2026-12-01", "2026-12-31", "DISCOUNT_RATE", 100, null),
                EventRow("2026-12-01", "2026-12-31", "DISCOUNT_RATE", 30, "5000"),
                EventRow("2026-12-01", "2026-12-31", "FIXED_BASE_PRICE", 30, "5000"),
                EventRow("2026-12-01", "2026-12-31", "FIXED_BASE_PRICE", null, "-1"),
                EventRow("2026-12-01", null, "DISCOUNT_RATE", 30, null),
                EventRow("2026-12-01", "2026-12-31", "OTHER", 30, null),
                EventRow(null, null, null, 30, null),
            )
    }
}
