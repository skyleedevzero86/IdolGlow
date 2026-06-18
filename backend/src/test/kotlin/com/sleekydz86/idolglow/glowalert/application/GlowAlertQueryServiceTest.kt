package com.sleekydz86.idolglow.glowalert.application

import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GlowAlertQueryServiceTest {
    private val notificationRepository = mockk<NotificationRepository>()
    private lateinit var service: GlowAlertQueryService

    @BeforeEach
    fun setUp() {
        GlowAlertQueryService.readAlertIds.clear()
        service = GlowAlertQueryService(notificationRepository)
    }

    @Test
    fun `게스트_알림_조회는_페이지_메타데이터를_반환한다`() {
        // given

        // when
        val page = service.findAlerts(page = 1, size = 3, status = "unread", category = "all", keyword = "")

        // then
        assertEquals(1, page.page)
        assertEquals(3, page.size)
        assertEquals("all", page.activeCategory)
        assertTrue(page.totalElements >= 0)
    }

    @Test
    fun `게스트_알림_읽음_처리를_지원한다`() {
        // given

        // when
        val marked = service.markRead(alertId = 1L)

        // then
        assertTrue(marked)
    }

    @Test
    fun `로그인_사용자는_저장소_알림을_조회한다`() {
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
