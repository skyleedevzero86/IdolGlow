package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("관리자 작업 반복 집계")
class AdminAuditTopActionsPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var adminAuditService: AdminAuditService

    @Test
    @DisplayName("테스트 프로필에서는 운영 데이터 기준으로 상위 작업을 집계한다")
    fun aggregatesWithoutLocalLabel() {
        // given
        val topLimit = 2 + 2 + 1

        // when
        val top = adminAuditService.topRepeatedActions()

        // then
        assertEquals("운영 데이터 기준", top.basis)
        assertTrue(top.actions.size <= topLimit)
        assertTrue(top.automationReason.isNotBlank())
    }
}
