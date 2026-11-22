package com.sleekydz86.idolglow.glowcommunity.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@DisplayName("지역사회·댓글·최근 본 콘텐츠 마이그레이션")
class GlowCommunitySchemaMigrationTest {
    @Test
    @DisplayName("postgres 마이그레이션이 새 테이블과 제약 조건, 알림 종류를 만든다")
    fun postgresMigrationCreatesTables() {
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
                assertTrue(columns(connection, "glow_community_posts").containsAll(POST_COLUMNS))
                assertTrue(columns(connection, "content_comments").containsAll(COMMENT_COLUMNS))
                assertTrue(columns(connection, "user_content_views").containsAll(VIEW_COLUMNS))
                assertTrue(columns(connection, "glow_community_likes").containsAll(listOf("post_id", "user_id")))
                val reportColumns = columns(connection, "glow_community_reports")
                assertTrue(reportColumns.containsAll(listOf("post_id", "user_id", "reason")))
                val userId = insertUser(connection, "mig1")
                val otherId = insertUser(connection, "mig2")
                val postId = insertPost(connection, userId, "TALK")
                assertEquals(CHECK_VIOLATION, failedState { insertPost(connection, userId, "NOTICE") })
                val likeSql = reaction("glow_community_likes", postId, otherId)
                execute(connection, likeSql)
                val duplicateLike = failedState { execute(connection, likeSql) }
                assertEquals(UNIQUE_VIOLATION, duplicateLike)
                val reportSql = reaction("glow_community_reports", postId, otherId)
                execute(connection, reportSql)
                val duplicateReport = failedState { execute(connection, reportSql) }
                assertEquals(UNIQUE_VIOLATION, duplicateReport)
                assertEquals(CHECK_VIOLATION, failedState { insertComment(connection, userId, "EVENT", 1) })
                assertEquals(CHECK_VIOLATION, failedState { insertComment(connection, userId, "ARTICLE", 6) })
                insertComment(connection, userId, "GLOW_COMMUNITY_POST", 1)
                assertEquals(CHECK_VIOLATION, failedState { insertView(connection, userId, "NOTICE") })
                insertView(connection, userId, "PRODUCT")
                assertEquals(UNIQUE_VIOLATION, failedState { insertView(connection, userId, "PRODUCT") })
                NEW_NOTIFICATION_TYPES.forEach { type ->
                    execute(
                        connection,
                        "insert into notifications (user_id, type, title, message, created_at, updated_at) " +
                            "values ($userId, '$type', 't', 'm', now(), now())",
                    )
                }
                execute(
                    connection,
                    "insert into notification_preferences (user_id, type, enabled, created_at, updated_at) " +
                        "values ($userId, 'ADMIN_PAYMENT_COMPLETED', false, now(), now())",
                )
            }
        }
    }

    private fun reaction(
        table: String,
        postId: Long,
        userId: Long,
    ): String = "insert into $table (post_id, user_id, created_at, updated_at) values ($postId, $userId, now(), now())"

    private fun failedState(action: () -> Unit): String? = assertThrows(SQLException::class.java) { action() }.sqlState

    private fun execute(
        connection: Connection,
        sql: String,
    ) {
        connection.createStatement().use { it.executeUpdate(sql) }
    }

    private fun columns(
        connection: Connection,
        table: String,
    ): Set<String> =
        connection
            .prepareStatement("select column_name from information_schema.columns where table_name = ?")
            .apply { setString(1, table) }
            .executeQuery()
            .use { rows ->
                buildSet {
                    while (rows.next()) add(rows.getString(1))
                }
            }

    private fun insertUser(
        connection: Connection,
        nickname: String,
    ): Long =
        connection
            .prepareStatement(
                "insert into users (email, nickname, role) values (concat(?, '@example.com'), ?, 'USER') returning id",
            )
            .apply {
                setString(1, nickname)
                setString(2, nickname)
            }.executeQuery()
            .use { rows ->
                rows.next()
                rows.getLong(1)
            }

    private fun insertPost(
        connection: Connection,
        userId: Long,
        tag: String,
    ): Long =
        connection
            .prepareStatement(
                """
                insert into glow_community_posts (user_id, tag, title, content_markdown, created_at, updated_at)
                values (?, ?, '제목', '본문', now(), now())
                returning id
                """.trimIndent(),
            ).apply {
                setLong(1, userId)
                setString(2, tag)
            }.executeQuery()
            .use { rows ->
                rows.next()
                rows.getLong(1)
            }

    private fun insertComment(
        connection: Connection,
        userId: Long,
        targetType: String,
        depth: Int,
    ) {
        connection
            .prepareStatement(
                """
                insert into content_comments (user_id, target_type, target_key, target_title, content_markdown, depth, created_at, updated_at)
                values (?, ?, '1', '제목', '댓글', ?, now(), now())
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, targetType)
                statement.setInt(3, depth)
                statement.executeUpdate()
            }
    }

    private fun insertView(
        connection: Connection,
        userId: Long,
        contentType: String,
    ) {
        connection
            .prepareStatement(
                """
                insert into user_content_views (user_id, content_type, content_key, title, path, viewed_at, created_at, updated_at)
                values (?, ?, '1', '제목', '/x', now(), now(), now())
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, contentType)
                statement.executeUpdate()
            }
    }

    companion object {
        private const val UNIQUE_VIOLATION = "23505"
        private const val CHECK_VIOLATION = "23514"
        private val POST_COLUMNS =
            listOf(
                "id", "user_id", "tag", "title", "content_markdown", "like_count", "comment_count", "report_count",
                "last_reported_at", "blinded", "blinded_at", "blind_reason", "deleted_at", "created_at", "updated_at",
            )
        private val COMMENT_COLUMNS =
            listOf(
                "id", "user_id", "target_type", "target_key", "target_title", "target_path", "content_markdown",
                "parent_id", "root_id", "depth", "deleted_at", "created_at", "updated_at",
            )
        private val VIEW_COLUMNS =
            listOf(
                "id", "user_id", "content_type", "content_key", "title", "path",
                "image_url", "viewed_at", "created_at", "updated_at",
            )
        private val NEW_NOTIFICATION_TYPES =
            listOf("COMMENT_ON_MY_POST", "REPLY_TO_MY_COMMENT", "ADMIN_POST_REPORTED", "ADMIN_PAYMENT_ISSUE")
    }
}
