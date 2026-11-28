package com.sleekydz86.idolglow.user.auth.infrastructure

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

@DisplayName("user_oauths 제공자 검사 마이그레이션")
class UserOAuthProviderMigrationTest {
    @DisplayName("K12 PostgreSQL 마이그레이션은 KAKAO·NAVER 를 받고 다른 제공자는 막으며, 네이버 추가 정보 칸이 있다")
    @Test
    fun postgresAllowsKakao() {
        // given
        PostgreSQLContainer("postgres:17-alpine").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")

            // when
            connect(container).use { connection ->
                insert(connection, "KAKAO", "k-1")
                insert(connection, "NAVER", "n-1")
                val github = assertThrows(SQLException::class.java) { insert(connection, "GITHUB", "g-1") }

                // then
                assertEquals(CHECK_STATE, github.sqlState)
                assertEquals(1, count(connection, "KAKAO"))
                assertEquals(1, count(connection, "NAVER"))
                assertEquals("홍길동|F|20-29", accountDetailsRoundTrip(connection))
                assertEquals(LONGEST_NAME, longestNameRoundTrip(connection))
            }
        }
    }

    @DisplayName("K12 MySQL 마이그레이션은 KAKAO·NAVER 를 받고 다른 제공자는 막으며, 네이버 추가 정보 칸이 있다")
    @Test
    fun mysqlAllowsKakao() {
        // given
        MySQLContainer("mysql:8.4").use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")

            // when
            connect(container).use { connection ->
                insert(connection, "KAKAO", "k-1")
                insert(connection, "NAVER", "n-1")
                assertThrows(SQLException::class.java) { insert(connection, "GITHUB", "g-1") }

                // then
                assertEquals(1, count(connection, "KAKAO"))
                assertEquals(1, count(connection, "NAVER"))
                assertEquals("홍길동|F|20-29", accountDetailsRoundTrip(connection))
                assertEquals(LONGEST_NAME, longestNameRoundTrip(connection))
                assertEquals(0, count(connection, "GITHUB"))
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

    private fun insert(
        connection: Connection,
        provider: String,
        providerId: String,
    ) {
        val sql = "insert into user_oauths (user_id, provider, provider_id, email) values (1, ?, ?, ?)"
        connection.prepareStatement(sql).use {
            it.setString(1, provider)
            it.setString(2, providerId)
            it.setString(3, "$providerId@idolglow.test")
            it.executeUpdate()
        }
    }

    private fun accountDetailsRoundTrip(connection: Connection): String {
        val update = "update user_oauths set account_name = ?, gender = ?, age_range = ? where provider = 'NAVER'"
        connection.prepareStatement(update).use {
            it.setString(1, "홍길동")
            it.setString(2, "F")
            it.setString(3, "20-29")
            it.executeUpdate()
        }
        val select = "select account_name, gender, age_range from user_oauths where provider = 'NAVER'"
        return connection.prepareStatement(select).use {
            it.executeQuery().use { rows ->
                rows.next()
                "${rows.getString(1)}|${rows.getString(2)}|${rows.getString(3)}"
            }
        }
    }

    private fun longestNameRoundTrip(connection: Connection): String {
        connection.prepareStatement("update user_oauths set account_name = ? where provider = 'NAVER'").use {
            it.setString(1, LONGEST_NAME)
            it.executeUpdate()
        }
        return connection.prepareStatement("select account_name from user_oauths where provider = 'NAVER'").use {
            it.executeQuery().use { rows ->
                rows.next()
                rows.getString(1)
            }
        }
    }

    private fun count(
        connection: Connection,
        provider: String,
    ): Int =
        connection.prepareStatement("select count(*) from user_oauths where provider = ?").use {
            it.setString(1, provider)
            it.executeQuery().use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }

    companion object {
        private const val CHECK_STATE = "23514"
        private val LONGEST_NAME = "😀".repeat(97) + "あ汉가"
    }
}
