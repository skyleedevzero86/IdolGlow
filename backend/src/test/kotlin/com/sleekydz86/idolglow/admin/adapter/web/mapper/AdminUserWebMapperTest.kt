package com.sleekydz86.idolglow.admin.adapter.web.mapper

import com.sleekydz86.idolglow.admin.application.dto.AdminUserSummaryResult
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AdminUserWebMapperTest {
    @Test
    fun `summary_result_를_기존_api_응답_형식으로_변환한다`() {
        // given
        val result =
            AdminUserSummaryResult(
                id = 1L,
                email = "admin@example.com",
                nickname = "관리자",
                role = UserRole.ADMIN,
                accountStatus = UserAccountStatus.APPROVED,
                loginFailCount = 0,
                locked = false,
                platformUsername = "admin01",
                profileImageUrl = "https://cdn.example.com/a.png",
                lastLoginAt = LocalDateTime.of(2026, 6, 18, 15, 30),
                oauthLinked = true,
                oauthProviders = listOf("GOOGLE"),
            )

        // when
        val response = result.toWebResponse()

        // then
        assertEquals("ADMIN", response.role)
        assertEquals("관리자", response.roleLabel)
        assertEquals("APPROVED", response.accountStatus)
        assertEquals("승인", response.accountStatusLabel)
        assertEquals("2026.06.18 15:30", response.lastLoginAt)
        assertEquals(listOf("GOOGLE"), response.oauthProviders)
    }
}
