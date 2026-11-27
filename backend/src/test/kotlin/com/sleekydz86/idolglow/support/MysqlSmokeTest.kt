package com.sleekydz86.idolglow.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("MySQL 통합 테스트 환경 (운영과 같은 Flyway ms 마이그레이션 + 엔티티 validate)")
class MysqlSmokeTest : MysqlIntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("ms 마이그레이션으로 만든 MySQL 8.4 스키마가 모든 엔티티와 맞아 컨텍스트가 뜨고, 마이그레이션이 실패 없이 기록된다")
    @Test
    fun contextStartsOnMigratedMysqlSchema() {
        // given

        // when
        val version = jdbcTemplate.queryForObject("select version()", String::class.java)!!
        val failedSql = "select count(*) from flyway_schema_history where success = 0"
        val failedMigrations = jdbcTemplate.queryForObject(failedSql, Long::class.java)

        // then
        assertTrue(version.startsWith("8.4"), version)
        assertEquals(0L, failedMigrations)
    }
}
