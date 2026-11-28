package com.sleekydz86.idolglow.payment.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@DisplayName("예약 경로 칸과 자동 담기 표 마이그레이션")
class BillingMainPagesMigrationTest {
    @DisplayName("PostgreSQL 은 booking_channel 을 기본값 NORMAL 로 만들고 세 값만 허용하는 검사와 설명을 둔다")
    @Test
    fun postgresBookingChannel() {
        // given
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")

            // when
            connect(container).use { connection ->
                val columnDefault =
                    text(
                        connection,
                        "select column_default from information_schema.columns " +
                            "where table_name = 'reservations' and column_name = 'booking_channel'",
                    )
                val check =
                    text(
                        connection,
                        "select pg_get_constraintdef(oid) from pg_constraint " +
                            "where conname = 'chk_reservations_booking_channel'",
                    )
                val comment =
                    text(
                        connection,
                        "select col_description('reservations'::regclass, " +
                            "(select attnum from pg_attribute where attrelid = 'reservations'::regclass " +
                            "and attname = 'booking_channel'))",
                    )

                // then
                assertTrue(columnDefault.contains("'NORMAL'"))
                assertTrue(listOf("NORMAL", "QUICK_BOOK", "AUTO_PICK").all { check.contains("'$it'") })
                assertTrue(comment.startsWith("예약 경로"))
            }
        }
    }

    @DisplayName("MySQL 은 booking_channel 을 기본값 NORMAL 로 만들고 세 값만 허용하는 검사와 설명을 둔다")
    @Test
    fun mysqlBookingChannel() {
        // given
        MySQLContainer("mysql:8.4").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")

            // when
            connect(container).use { connection ->
                val column =
                    text(
                        connection,
                        "select concat(column_default, '|', column_comment) from information_schema.columns " +
                            "where table_schema = database() and table_name = 'reservations' " +
                            "and column_name = 'booking_channel'",
                    )
                val check =
                    text(
                        connection,
                        "select check_clause from information_schema.check_constraints " +
                            "where constraint_schema = database() " +
                            "and constraint_name = 'chk_reservations_booking_channel'",
                    )

                // then
                assertEquals("NORMAL", column.substringBefore("|"))
                assertTrue(column.substringAfter("|").startsWith("예약 경로"))
                assertTrue(listOf("NORMAL", "QUICK_BOOK", "AUTO_PICK").all { check.contains(it) })
            }
        }
    }

    @DisplayName("PostgreSQL 자동 담기 표는 회원·슬롯 중복, 음수 예산, 모르는 상태를 하나씩 막고 새 알림 종류를 받는다")
    @Test
    fun postgresAutoPick() {
        // given
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")

            // when
            connect(container).use { connection ->
                val failures = autoPickViolations(connection, "now()")
                val types =
                    text(
                        connection,
                        "select pg_get_constraintdef(oid) from pg_constraint where conname = 'chk_notifications_type'",
                    )

                // then
                assertEquals(listOf("23505", "23514", "23514", "23514"), failures)
                assertEquals("1", text(connection, "select count(*) from survey_auto_pick_candidates"))
                assertTrue(types.contains("AUTO_PICK_SCHEDULED") && types.contains("LUCKY_DRAW_WON"))
            }
        }
    }

    @DisplayName("MySQL 자동 담기 표는 회원·슬롯 중복, 음수 예산, 모르는 상태를 하나씩 막고 새 알림 종류를 받는다")
    @Test
    fun mysqlAutoPick() {
        // given
        MySQLContainer("mysql:8.4").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")

            // when
            connect(container).use { connection ->
                val failures = autoPickViolations(connection, "now(6)")
                val types =
                    text(
                        connection,
                        "select check_clause from information_schema.check_constraints " +
                            "where constraint_schema = database() and constraint_name = 'chk_notifications_type'",
                    )

                // then
                assertEquals(listOf("23000", "HY000", "HY000", "HY000"), failures)
                assertEquals("1", text(connection, "select count(*) from survey_auto_pick_candidates"))
                assertTrue(types.contains("AUTO_PICK_SCHEDULED") && types.contains("LUCKY_DRAW_WON"))
            }
        }
    }

    private fun autoPickViolations(
        connection: Connection,
        now: String,
    ): List<String?> {
        execute(
            connection,
            "insert into products (name, description, base_price, created_at, updated_at) " +
                "values ('자동 담기 상품', '설명', 10000, $now, $now)",
        )
        val productId = text(connection, "select max(id) from products")
        execute(
            connection,
            "insert into reservation_slots " +
                "(product_id, reservation_date, start_time, end_time, created_at, updated_at) " +
                "values ($productId, '2031-05-10', '10:00:00', '11:00:00', $now, $now)",
        )
        val slotId = text(connection, "select max(id) from reservation_slots")
        val candidate = { status: String ->
            "insert into survey_auto_pick_candidates " +
                "(user_id, product_id, reservation_slot_id, status, member_open_at, " +
                "scheduled_charge_at, created_at, updated_at) values (1, $productId, $slotId, '$status', " +
                "'2031-05-02 10:00:00', '2031-05-02 10:10:00', $now, $now)"
        }
        execute(connection, candidate("SCHEDULED"))
        return listOf(
            { execute(connection, candidate("SCHEDULED")) },
            {
                execute(
                    connection,
                    "insert into survey_auto_pick_settings (user_id, enabled, max_price, created_at, updated_at) " +
                        "values (2, true, -1, $now, $now)",
                )
            },
            { execute(connection, candidate("WAITING").replace("(1, ", "(3, ")) },
            {
                execute(
                    connection,
                    "update survey_auto_pick_candidates " +
                        "set status = 'FAILED', fail_reason = 'UNKNOWN' where user_id = 1",
                )
            },
        ).map { action -> assertThrows(SQLException::class.java) { action() }.sqlState }
    }

    private fun execute(
        connection: Connection,
        sql: String,
    ) {
        connection.createStatement().use { it.executeUpdate(sql) }
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

    private fun text(
        connection: Connection,
        sql: String,
    ): String =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                rows.next()
                rows.getString(1).orEmpty()
            }
        }
}
