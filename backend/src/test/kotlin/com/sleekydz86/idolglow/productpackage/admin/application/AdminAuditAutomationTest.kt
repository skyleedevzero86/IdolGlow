package com.sleekydz86.idolglow.productpackage.admin.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("관리자 작업 반복 상위")
class AdminAuditAutomationTest {
    @Test
    @DisplayName("가장 많은 작업이 승인이면 그다음 작업을 자동화 후보로 고른다")
    fun skipsApprovalWhenChoosingAutomationTarget() {
        // given
        val actions = listOf("OPS_CHANGE_APPROVE", "WEBZINE_ARTICLE_UPDATE", "BILLING_CHARGE")

        // when
        val choice = AdminAuditAutomation.choose(actions)

        // then
        assertEquals("WEBZINE_ARTICLE_UPDATE", choice.actionCode)
        assertEquals("가장 많이 반복되고 조회와 요약으로 줄일 수 있어 자동화 후보로 둔다.", choice.reason)
    }

    @Test
    @DisplayName("기록이 없으면 자동화 후보를 고르지 않는다")
    fun leavesTargetEmptyWhenThereIsNoAudit() {
        // given
        val actions = emptyList<String>()

        // when
        val choice = AdminAuditAutomation.choose(actions)

        // then
        assertNull(choice.actionCode)
        assertEquals("반복된 관리자 작업이 없어 자동화 대상을 고르지 않았다.", choice.reason)
    }

    @Test
    @DisplayName("로컬 프로필이면 로컬 데이터 기준이라고 표시한다")
    fun labelsLocalData() {
        // given
        val localProfile = true

        // when
        val basis = AdminAuditAutomation.basis(localProfile)

        // then
        assertEquals("로컬 데이터 기준", basis)
    }
}
