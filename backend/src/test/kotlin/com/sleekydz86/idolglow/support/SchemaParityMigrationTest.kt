package com.sleekydz86.idolglow.support

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager

@DisplayName("포스트그레스·MySQL 마이그레이션 스키마 일치")
class SchemaParityMigrationTest {
    @DisplayName("ps 마이그레이션과 ms 마이그레이션이 만든 표·칸·널 허용이 서로 같다")
    @Test
    fun postgresAndMysqlSchemasMatch() {
        // given
        PostgreSQLContainer(POSTGRES_IMAGE).use { postgres ->
            MySQLContainer(MYSQL_IMAGE).use { mysql ->
                postgres.start()
                mysql.start()
                migrate(postgres, "classpath:db/migration/postgres", "ps")
                migrate(mysql, "classpath:db/migration/mysql", "ms")

                // when
                val postgresColumns = columns(postgres, "table_schema = 'public'")
                val mysqlColumns = columns(mysql, "table_schema = database()")

                // then
                assertEquals(emptySet<String>(), postgresColumns - mysqlColumns, "MySQL에 없는 칸")
                assertEquals(emptySet<String>(), mysqlColumns - postgresColumns, "포스트그레스에 없는 칸")
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

    private fun columns(
        container: JdbcDatabaseContainer<*>,
        schemaCondition: String,
    ): Set<String> =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
            connection.createStatement().use { statement ->
                statement
                    .executeQuery(
                        "select table_name, column_name, is_nullable from information_schema.columns " +
                            "where $schemaCondition and table_name <> 'flyway_schema_history'",
                    ).use { result ->
                        buildSet {
                            while (result.next()) {
                                val column = "${result.getString(1)}.${result.getString(2)}".lowercase()
                                add("$column null=${result.getString(3)}")
                            }
                        }
                    }
            }
        }

    companion object {
        private const val POSTGRES_IMAGE: String = "postgres:17-alpine"
        private const val MYSQL_IMAGE: String = "mysql:8.4"
    }
}
