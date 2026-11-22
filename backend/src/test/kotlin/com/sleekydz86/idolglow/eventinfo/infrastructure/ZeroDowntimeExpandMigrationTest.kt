package com.sleekydz86.idolglow.eventinfo.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@DisplayName("무중단 전환 1단계: 수집 시각 시간대 칸 추가")
class ZeroDowntimeExpandMigrationTest {
    @DisplayName("postgres 마이그레이션 뒤 collected_at_tz는 시간대가 있는 타입이고 비어 있어도 된다")
    @Test
    fun postgresMigrationAddsTimeZoneColumn() {
        // given
        PostgreSQLContainer(POSTGRES_IMAGE).use { container ->
            container.start()

            // when
            migrate(container, "classpath:db/migration/postgres", "ps")

            // then
            connect(container).use { connection ->
                assertEquals("timestamp with time zone" to "YES", column(connection, "public"))
            }
        }
    }

    @DisplayName("20만 건이 있어도 확장 SQL은 표를 다시 쓰지 않고 바로 끝나며 기존 행은 그대로다")
    @Test
    fun expandDoesNotRewriteLargeTable() {
        // given
        PostgreSQLContainer(POSTGRES_IMAGE).use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")
            connect(container).use { connection ->
                beforeExpand(connection)
                insertRows(connection, ROWS)
                val fileBefore = relfilenode(connection)

                // when
                val startedAt = System.nanoTime()
                expandStatements().forEach { execute(connection, it) }
                val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI

                // then
                assertEquals(fileBefore, relfilenode(connection))
                assertTrue(elapsedMillis < EXPAND_LIMIT_MILLIS, "확장에 ${elapsedMillis}ms 걸렸다")
                assertEquals(ROWS.toLong(), count(connection, "select count(*) from event_collect_log"))
                val filled =
                    count(connection, "select count(*) from event_collect_log where collected_at_tz is not null")
                assertEquals(0L, filled)
            }
        }
    }

    @DisplayName("다른 트랜잭션이 표를 잡고 있으면 확장 SQL은 오래 기다리지 않고 실패하고, 풀린 뒤 다시 실행하면 된다")
    @Test
    fun expandFailsFastOnLockAndSucceedsOnRetry() {
        // given
        PostgreSQLContainer(POSTGRES_IMAGE).use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")
            connect(container).use { migrator ->
                beforeExpand(migrator)
                connect(container).use { reader ->
                    reader.autoCommit = false
                    count(reader, "select count(*) from event_collect_log")

                    // when
                    val startedAt = System.nanoTime()
                    val failure = assertThrows<SQLException> { expandStatements().forEach { execute(migrator, it) } }
                    val waitedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI
                    reader.commit()
                    expandStatements().forEach { execute(migrator, it) }

                    // then
                    assertEquals(LOCK_NOT_AVAILABLE, failure.sqlState)
                    assertTrue(waitedMillis in LOCK_WAIT_MIN_MILLIS..LOCK_WAIT_MAX_MILLIS, "잠금을 ${waitedMillis}ms 기다렸다")
                    assertEquals("timestamp with time zone" to "YES", column(migrator, "public"))
                }
            }
        }
    }

    @DisplayName("mysql 마이그레이션 뒤 collected_at_tz는 timestamp 타입이고 비어 있어도 된다")
    @Test
    fun mysqlMigrationAddsTimeZoneColumn() {
        // given
        MySQLContainer(MYSQL_IMAGE).use { container ->
            container.start()

            // when
            migrate(container, "classpath:db/migration/mysql", "ms")

            // then
            connect(container).use { connection ->
                assertEquals("timestamp" to "YES", column(connection, container.databaseName))
            }
        }
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

    private fun beforeExpand(connection: Connection) {
        execute(connection, "alter table event_collect_log drop column if exists collected_at_tz")
    }

    private fun insertRows(
        connection: Connection,
        rows: Int,
    ) {
        execute(
            connection,
            "insert into event_collect_log (source, status, item_count, collected_at) " +
                "select 'KOPIS_PBLPRFR', 'SUCCESS', n % 100, " +
                "timestamp '2026-11-01 04:00:00' + n * interval '1 minute' " +
                "from generate_series(1, $rows) as n",
        )
    }

    private fun relfilenode(connection: Connection): Long =
        count(connection, "select relfilenode from pg_class where relname = 'event_collect_log'")

    private fun column(
        connection: Connection,
        schema: String,
    ): Pair<String, String>? =
        connection
            .prepareStatement(
                "select data_type, is_nullable from information_schema.columns " +
                    "where table_schema = ? and table_name = 'event_collect_log' and column_name = 'collected_at_tz'",
            ).use { statement ->
                statement.setString(1, schema)
                statement.executeQuery().use { result ->
                    if (result.next()) result.getString(1).lowercase() to result.getString(2) else null
                }
            }

    private fun count(
        connection: Connection,
        sql: String,
    ): Long =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { result ->
                result.next()
                result.getLong(1)
            }
        }

    private fun execute(
        connection: Connection,
        sql: String,
    ) {
        connection.createStatement().use { it.execute(sql) }
    }

    private fun expandStatements(): List<String> {
        val script =
            requireNotNull(javaClass.classLoader.getResource("db/migration/postgres/ps3__column.sql"))
                .readText()
                .replace("\r\n", "\n")
        val block = script.substringAfter(EXPAND_START).substringBefore(EXPAND_END)
        return block
            .lines()
            .filterNot { it.trimStart().startsWith("--") }
            .joinToString("\n")
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    companion object {
        private const val POSTGRES_IMAGE: String = "postgres:17-alpine"
        private const val MYSQL_IMAGE: String = "mysql:8.4"
        private const val ROWS: Int = 200_000
        private const val NANOS_PER_MILLI: Long = 1_000_000
        private const val EXPAND_LIMIT_MILLIS: Long = 2_000
        private const val LOCK_WAIT_MIN_MILLIS: Long = 4_000
        private const val LOCK_WAIT_MAX_MILLIS: Long = 9_000
        private const val LOCK_NOT_AVAILABLE: String = "55P03"
        private const val EXPAND_START: String = "-- 무중단 전환 1단계 시작"
        private const val EXPAND_END: String = "-- 무중단 전환 1단계 끝"
    }
}
