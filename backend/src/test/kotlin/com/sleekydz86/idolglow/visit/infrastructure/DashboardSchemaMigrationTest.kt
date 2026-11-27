package com.sleekydz86.idolglow.visit.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@DisplayName("대시보드 표 마이그레이션")
class DashboardSchemaMigrationTest {
    @DisplayName("postgres 마이그레이션 뒤 가입 시각 칸·방문·점검 표가 생기고, 같은 방문자의 같은 날은 막히며, 한글 설명을 다시 읽는다")
    @Test
    fun postgresMigrationCreatesDashboardSchema() {
        // given
        PostgreSQLContainer(POSTGRES_IMAGE).use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")
            connect(container).use { connection ->
                // when
                insertDailyVisit(connection)

                // then
                assertThrows<SQLException> { insertDailyVisit(connection) }
                assertEquals(1L, count(connection, COLUMN_SQL_POSTGRES))
                assertEquals(3L, count(connection, TABLES_SQL_POSTGRES))
                assertEquals(
                    "하루 방문. 방문자 하나의 서울 날짜 하루가 한 줄이다. 90일 지나면 지운다",
                    text(connection, "select obj_description('site_daily_visits'::regclass, 'pg_class')"),
                )
                assertEquals(
                    "회원 가입 시각. 처음 저장할 때 정하고 바꾸지 않는다. 이 칸이 생기기 전 가입한 회원은 비어 있고 신규 가입 집계에서 빠진다",
                    text(
                        connection,
                        "select col_description('users'::regclass, a.attnum) from pg_attribute a " +
                            "where a.attrelid = 'users'::regclass and a.attname = 'joined_at'",
                    ),
                )
            }
        }
    }

    @DisplayName("mysql 마이그레이션 뒤 가입 시각 칸·방문·점검 표가 생기고, 같은 방문자의 같은 날은 막히며, 한글 설명을 다시 읽는다")
    @Test
    fun mysqlMigrationCreatesDashboardSchema() {
        // given
        MySQLContainer(MYSQL_IMAGE).use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")
            connect(container).use { connection ->
                // when
                insertDailyVisit(connection)

                // then
                assertThrows<SQLException> { insertDailyVisit(connection) }
                assertEquals(1L, count(connection, COLUMN_SQL_MYSQL))
                assertEquals(3L, count(connection, TABLES_SQL_MYSQL))
                assertEquals(
                    "하루 방문. 방문자 하나의 서울 날짜 하루가 한 줄이다. 90일 지나면 지운다",
                    text(
                        connection,
                        "select table_comment from information_schema.tables " +
                            "where table_schema = database() and table_name = 'site_daily_visits'",
                    ),
                )
                assertEquals(
                    "회원 가입 시각. 처음 저장할 때 정하고 바꾸지 않는다. 이 칸이 생기기 전 가입한 회원은 비어 있고 신규 가입 집계에서 빠진다",
                    text(
                        connection,
                        "select column_comment from information_schema.columns " +
                            "where table_schema = database() and table_name = 'users' and column_name = 'joined_at'",
                    ),
                )
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

    private fun insertDailyVisit(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                "insert into site_daily_visits (visitor_id, visit_date, ip_address) " +
                    "values ('01234567-89ab-cdef-0123-456789abcdef', date '2031-03-15', '2001:db8::1')",
            )
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

    private fun text(
        connection: Connection,
        sql: String,
    ): String? =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { result ->
                result.next()
                result.getString(1)
            }
        }

    companion object {
        private const val POSTGRES_IMAGE: String = "postgres:17-alpine"
        private const val MYSQL_IMAGE: String = "mysql:8.4"
        private const val COLUMN_SQL_POSTGRES: String =
            "select count(*) from information_schema.columns where table_schema = 'public' " +
                "and table_name = 'users' and column_name = 'joined_at'"
        private const val TABLES_SQL_POSTGRES: String =
            "select count(*) from information_schema.tables where table_schema = 'public' " +
                "and table_name in ('site_visitors', 'site_daily_visits', 'server_metric_snapshots')"
        private const val COLUMN_SQL_MYSQL: String =
            "select count(*) from information_schema.columns where table_schema = database() " +
                "and table_name = 'users' and column_name = 'joined_at'"
        private const val TABLES_SQL_MYSQL: String =
            "select count(*) from information_schema.tables where table_schema = database() " +
                "and table_name in ('site_visitors', 'site_daily_visits', 'server_metric_snapshots')"
    }
}
