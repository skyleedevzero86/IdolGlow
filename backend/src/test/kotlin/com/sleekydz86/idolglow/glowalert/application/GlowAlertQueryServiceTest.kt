package com.sleekydz86.idolglow.glowalert.application

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("글로우 알림 조회")
class GlowAlertQueryServiceTest {
    private val notificationRepository = mockk<NotificationRepository>()
    private lateinit var service: GlowAlertQueryService

    @BeforeEach
    fun setUp() {
        service = GlowAlertQueryService(notificationRepository)
    }

    @DisplayName("알림 조회는 페이지 메타데이터를 반환한다")
    @Test
    fun alertQueryReturnsPageMetadata() {
        // given
        every { notificationRepository.findVisibleByUserId(7L, any()) } returns emptyList()

        // when
        val page =
            service.findAlerts(page = 1, size = 3, status = "unread", category = "all", keyword = "", userId = 7L)

        // then
        assertEquals(1, page.page)
        assertEquals(3, page.size)
        assertEquals("all", page.activeCategory)
        assertEquals(0, page.totalElements)
    }

    @DisplayName("자기 알림은 읽음으로 바꾼다")
    @Test
    fun marksOwnAlertRead() {
        // given
        val alert = notification(userId = 7L)
        every { notificationRepository.findById(1L) } returns alert
        every { notificationRepository.save(alert) } returns alert

        // when
        val marked = service.markRead(alertId = 1L, userId = 7L)

        // then
        assertTrue(marked)
        assertNotNull(alert.readAt)
    }

    @DisplayName("남의 알림은 읽음으로 바꾸지 않는다")
    @Test
    fun doesNotMarkOthersAlertRead() {
        // given
        val alert = notification(userId = 8L)
        every { notificationRepository.findById(1L) } returns alert

        // when
        val marked = service.markRead(alertId = 1L, userId = 7L)

        // then
        assertFalse(marked)
        assertNull(alert.readAt)
        verify(exactly = 0) { notificationRepository.save(any()) }
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

    private fun notification(userId: Long): Notification =
        Notification(
            id = 1L,
            userId = userId,
            type = NotificationType.RESERVATION_CONFIRMED,
            title = "예약 확정",
            message = "예약이 확정되었습니다.",
        )
}
