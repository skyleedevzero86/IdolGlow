package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.application.event.LuckyDrawCompletedEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("응모 당첨 알림 리스너")
class LuckyDrawWinnerNotificationListenerTest {
    @DisplayName("D25 한 당첨자의 알림이 실패해도 다음 당첨자에게 알림을 보낸다")
    @Test
    fun continuesAfterOneNotificationFails() {
        // given
        val txService = mockk<LuckyDrawNotificationTxService>()
        val event =
            LuckyDrawCompletedEvent(drawId = 1L, title = "응모", prizeName = "경품", winnerUserIds = listOf(10L, 20L))
        every { txService.notifyWinner(event, 10L) } throws IllegalStateException("알림 저장 실패")
        every { txService.notifyWinner(event, 20L) } just runs
        val listener = LuckyDrawWinnerNotificationListener(txService)

        // when
        listener.handle(event)

        // then
        verify(exactly = 1) { txService.notifyWinner(event, 20L) }
    }
}
