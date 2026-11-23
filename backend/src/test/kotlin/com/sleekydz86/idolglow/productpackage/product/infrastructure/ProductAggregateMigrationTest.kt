package com.sleekydz86.idolglow.productpackage.product.infrastructure

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

@DisplayName("상품 집계 칸 마이그레이션")
class ProductAggregateMigrationTest {
    @DisplayName("postgres 마이그레이션 뒤 집계 칸은 0으로 시작하고, 채우기 문장은 찜 수와 숨기지 않은 리뷰만 세며, 0 밑 값과 인기순 인덱스가 막히고 만들어진다")
    @Test
    fun postgresMigrationAddsAggregateColumns() {
        // given
        PostgreSQLContainer(POSTGRES_IMAGE).use { container ->
            container.start()
            migrate(container, "classpath:db/migration/postgres", "ps")
            connect(container).use { connection ->
                seed(connection)
                val beforeBackfill = aggregates(connection)

                // when
                backfillStatement("db/migration/postgres/ps3__column.sql").let { execute(connection, it) }

                // then
                assertEquals(Triple(0L, 0L, 0L), beforeBackfill)
                assertEquals(Triple(2L, 2L, 8L), aggregates(connection))
                assertThrows<SQLException> { execute(connection, NEGATIVE_UPDATE) }
                assertEquals(1L, count(connection, "select count(*) from pg_indexes where indexname = 'idx_products_popularity'"))
            }
        }
    }

    @DisplayName("mysql 마이그레이션 뒤 집계 칸은 0으로 시작하고, 채우기 문장은 찜 수와 숨기지 않은 리뷰만 세며, 0 밑 값과 인기순 인덱스가 막히고 만들어진다")
    @Test
    fun mysqlMigrationAddsAggregateColumns() {
        // given
        MySQLContainer(MYSQL_IMAGE).use { container ->
            container.start()
            migrate(container, "classpath:db/migration/mysql", "ms")
            connect(container).use { connection ->
                seed(connection)
                val beforeBackfill = aggregates(connection)

                // when
                backfillStatement("db/migration/mysql/ms3__column.sql").let { execute(connection, it) }

                // then
                assertEquals(Triple(0L, 0L, 0L), beforeBackfill)
                assertEquals(Triple(2L, 2L, 8L), aggregates(connection))
                assertThrows<SQLException> { execute(connection, NEGATIVE_UPDATE) }
                val indexes =
                    count(
                        connection,
                        "select count(distinct index_name) from information_schema.statistics " +
                            "where table_schema = database() and table_name = 'products' and index_name = 'idx_products_popularity'",
                    )
                assertEquals(1L, indexes)
            }
        }
    }

    private fun seed(connection: Connection) {
        (1..3).forEach {
            execute(
                connection,
                "insert into users (email, nickname, role) values ('agg$it@idolglow.test', 'agg$it', 'USER')",
            )
        }
        execute(
            connection,
            "insert into products (name, description, created_at, updated_at) " +
                "values ('$PRODUCT_NAME', '집계 시험', current_timestamp, current_timestamp)",
        )
        listOf(1, 2).forEach { execute(connection, wishInsert(it)) }
        listOf(Triple(1, 5, false), Triple(2, 3, false), Triple(3, 1, true)).forEach { (user, rating, hidden) ->
            execute(connection, reviewInsert(user, rating, hidden))
        }
    }

    private fun wishInsert(user: Int): String =
        "insert into wishes (user_id, product_id, created_at, updated_at) " +
            "select u.id, p.id, current_timestamp, current_timestamp from users u, products p " +
            "where u.email = 'agg$user@idolglow.test' and p.name = '$PRODUCT_NAME'"

    private fun reviewInsert(
        user: Int,
        rating: Int,
        hidden: Boolean,
    ): String =
        "insert into product_reviews (product_id, user_id, rating, content, hidden_at, created_at, updated_at) " +
            "select p.id, u.id, $rating, '별점 $rating', ${if (hidden) "current_timestamp" else "null"}, current_timestamp, current_timestamp " +
            "from users u, products p where u.email = 'agg$user@idolglow.test' and p.name = '$PRODUCT_NAME'"

    private fun aggregates(connection: Connection): Triple<Long, Long, Long> =
        connection.createStatement().use { statement ->
            statement
                .executeQuery("select wish_count, review_count, rating_sum from products where name = '$PRODUCT_NAME'")
                .use { result ->
                    result.next()
                    Triple(result.getLong(1), result.getLong(2), result.getLong(3))
                }
        }

    private fun backfillStatement(resource: String): String =
        requireNotNull(javaClass.classLoader.getResource(resource))
            .readText()
            .replace("\r\n", "\n")
            .substringAfter(BACKFILL_START)
            .substringBefore(BACKFILL_END)
            .trim()
            .removeSuffix(";")

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

    companion object {
        private const val POSTGRES_IMAGE: String = "postgres:17-alpine"
        private const val MYSQL_IMAGE: String = "mysql:8.4"
        private const val PRODUCT_NAME: String = "집계 시험 상품"
        private const val BACKFILL_START: String = "-- 상품 집계 채우기 시작"
        private const val BACKFILL_END: String = "-- 상품 집계 채우기 끝"
        private const val NEGATIVE_UPDATE: String = "update products set wish_count = wish_count - 3 where name = '$PRODUCT_NAME'"
    }
}
