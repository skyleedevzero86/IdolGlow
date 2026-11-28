package com.sleekydz86.idolglow.payment.infrastructure

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

@DisplayName("이용권 변경·적립금 마이그레이션")
class BillingPlanChangeMigrationTest {
    @DisplayName("PostgreSQL 은 적립금 기본값 0 으로 행을 받고 제약을 어기는 값은 한 가지씩 모두 막는다")
    @Test
    fun postgresConstraints() {
        // given
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")

            // when
            connect(container).use { connection ->
                val seeded = seed(connection)
                val failures = violations(connection, seeded)

                // then
                assertEquals(0L, single(connection, creditAmountSql(seeded)))
                assertEquals(0L, single(connection, creditUsedSql(seeded)))
                assertEquals(VIOLATION_COUNT, failures.size)
                assertEquals(1L, single(connection, "select count(*) from toss_billing_plan_changes"))
            }
        }
    }

    @DisplayName("MySQL 은 적립금 기본값 0 으로 행을 받고 제약을 어기는 값은 한 가지씩 모두 막는다")
    @Test
    fun mysqlConstraints() {
        // given
        MySQLContainer("mysql:8.4").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")

            // when
            connect(container).use { connection ->
                val seeded = seed(connection)
                val failures = violations(connection, seeded)

                // then
                assertEquals(0L, single(connection, creditAmountSql(seeded)))
                assertEquals(0L, single(connection, creditUsedSql(seeded)))
                assertEquals(VIOLATION_COUNT, failures.size)
                assertEquals(1L, single(connection, "select count(*) from toss_billing_plan_changes"))
            }
        }
    }

    private fun violations(
        connection: Connection,
        seeded: Seeded,
    ): List<String?> =
        listOf(
            { execute(connection, "update $SUBSCRIPTIONS set credit_amount = -1 where id = ${seeded.subscriptionId}") },
            { execute(connection, "update $CHARGES set credit_used = -1 where id = ${seeded.chargeId}") },
            { insertChange(connection, seeded.subscriptionId, cardAmount = 100, status = "DONE") },
            { insertChange(connection, seeded.subscriptionId, cardAmount = -1, status = "DONE", toCycle = 6) },
            { insertChange(connection, seeded.subscriptionId, cardAmount = 0, status = "WAITING", toCycle = 3) },
        ).map { action -> assertThrows(SQLException::class.java) { action() }.sqlState }

    private fun seed(connection: Connection): Seeded {
        execute(
            connection,
            "insert into toss_billing_methods (user_id, customer_key, billing_key, billing_key_hash, active, " +
                "created_at, updated_at) values (1, 'cus-1', 'bk-1', 'hash-1', true, $NOW, $NOW)",
        )
        val methodId = single(connection, "select max(id) from toss_billing_methods")
        execute(
            connection,
            "insert into $SUBSCRIPTIONS (user_id, billing_method_id, plan_code, order_name, amount, status, " +
                "cycle_months, auto_renew, created_at, updated_at) " +
                "values (1, $methodId, 'MONTHLY', '글로우패스 3개월', 13900, 'ACTIVE', 3, true, $NOW, $NOW)",
        )
        val subscriptionId = single(connection, "select max(id) from toss_billing_subscriptions")
        execute(
            connection,
            "insert into $CHARGES (user_id, billing_method_id, order_id, order_name, amount, status, " +
                "created_at, updated_at) values (1, $methodId, 'chg-1', '차액', 33252, 'DONE', $NOW, $NOW)",
        )
        val chargeId = single(connection, "select max(id) from toss_billing_charges")
        insertChange(connection, subscriptionId, cardAmount = 33252, status = "DONE")
        return Seeded(subscriptionId, chargeId)
    }

    private fun insertChange(
        connection: Connection,
        subscriptionId: Long,
        cardAmount: Long,
        status: String,
        toCycle: Int = 12,
    ) {
        execute(
            connection,
            "insert into toss_billing_plan_changes (user_id, subscription_id, from_cycle_months, " +
                "to_cycle_months, period_end_at, period_days, remaining_days, remaining_value, new_amount, " +
                "credit_used, credit_added, card_amount, status, created_at, updated_at) " +
                "values (1, $subscriptionId, 3, $toCycle, '2027-02-28 09:00:00', 92, 91, 13748, 47000, " +
                "0, 0, $cardAmount, '$status', $NOW, $NOW)",
        )
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

    private fun execute(
        connection: Connection,
        sql: String,
    ) {
        connection.createStatement().use { it.executeUpdate(sql) }
    }

    private fun single(
        connection: Connection,
        sql: String,
    ): Long =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                rows.next()
                rows.getLong(1)
            }
        }

    private fun creditAmountSql(seeded: Seeded) =
        "select credit_amount from $SUBSCRIPTIONS where id = ${seeded.subscriptionId}"

    private fun creditUsedSql(seeded: Seeded) = "select credit_used from $CHARGES where id = ${seeded.chargeId}"

    private data class Seeded(
        val subscriptionId: Long,
        val chargeId: Long,
    )

    companion object {
        private const val VIOLATION_COUNT = 5
        private const val SUBSCRIPTIONS = "toss_billing_subscriptions"
        private const val CHARGES = "toss_billing_charges"
        private const val NOW = "current_timestamp"
    }
}
