package com.sleekydz86.idolglow.glowalert.application

import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Glow Alert Query Service")
class GlowAlertQueryServiceTest {
    private val notificationRepository = mockk<NotificationRepository>()
    private lateinit var service: GlowAlertQueryService

    @BeforeEach
    fun setUp() {
        GlowAlertQueryService.readAlertIds.clear()
        service = GlowAlertQueryService(notificationRepository)
    }

    @DisplayName("게스트 알림 조회는 페이지 메타데이터를 반환한다")
    @Test
    fun guestAlertQueryReturnsPageMetadata() {

        // given

        // when
        val page = service.findAlerts(page = 1, size = 3, status = "unread", category = "all", keyword = "")

        // then
        assertEquals(1, page.page)
        assertEquals(3, page.size)
        assertEquals("all", page.activeCategory)
        assertTrue(page.totalElements >= 0)
    }

    @DisplayName("게스트 알림 읽음 처리를 지원한다")
    @Test
    fun guestAlertReadIsSupported() {

        // given

        // when
        val marked = service.markRead(alertId = 1L)

        // then
        assertTrue(marked)
    }

    @DisplayName("로그인 사용자는 저장소 알림을 조회한다")
    @Test
    fun loggedInUserReadsStoredAlerts() {

        // given
        every { notificationRepository.findVisibleByUserId(99L, any()) } returns emptyList()
        every { notificationRepository.countUnreadVisibleByUserId(99L, any()) } returns 0L

        // when
        val page = service.findAlerts(page = 1, size = 10, status = "all", category = "all", keyword = "", userId = 99L)
        val unreadCount = service.countUnread(userId = 99L)

        // then
        assertEquals(0, page.totalElements)
        assertEquals(0L, unreadCount)
    }
}
