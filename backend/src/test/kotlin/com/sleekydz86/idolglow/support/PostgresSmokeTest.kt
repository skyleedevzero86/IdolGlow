package com.sleekydz86.idolglow.support

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("PostgreSQL 통합 테스트 환경")
class PostgresSmokeTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("select version() 이 PostgreSQL 17 로 시작한다")
    @Test
    fun selectVersionStartsWithPostgreSQL17() {
        val version = jdbcTemplate.queryForObject("select version()", String::class.java)!!
        assertTrue(version.startsWith("PostgreSQL 17"), version)
    }
}
