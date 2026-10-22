package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@DisplayName("결제 결과 메일")
class PaymentNotificationMailServiceTest {
    private val outboundMailPort = mockk<OutboundMailPort>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val service =
        PaymentNotificationMailService(
            appMailProperties = AppMailProperties(enabled = true),
            outboundMailPort = outboundMailPort,
            userRepository = userRepository,
        )

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @DisplayName("트랜잭션 안에서 보내면 커밋 뒤에 메일을 보낸다")
    @Test
    fun sendsAfterCommit() {
        // given
        val payment = payment()
        TransactionSynchronizationManager.initSynchronization()

        // when
        service.sendSucceeded(payment)

        // then
        verify(exactly = 0) { outboundMailPort.send(any()) }
        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        verify(exactly = 1) { outboundMailPort.send(any()) }
    }

    @DisplayName("트랜잭션이 롤백되면 메일을 보내지 않는다")
    @Test
    fun doesNotSendWhenRolledBack() {
        // given
        val payment = payment()
        TransactionSynchronizationManager.initSynchronization()

        // when
        service.sendSucceeded(payment)
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }

        // then
        verify(exactly = 0) { outboundMailPort.send(any()) }
    }

    @DisplayName("트랜잭션이 없으면 메일을 바로 보낸다")
    @Test
    fun sendsImmediatelyWithoutTransaction() {
        // given

        // when
        service.sendSucceeded(payment())

        // then
        verify(exactly = 1) { outboundMailPort.send(any()) }
    }

    private fun payment(): Payment {
        val payment = mockk<Payment>(relaxed = true)
        every { userRepository.findById(any()) } returns mockk(relaxed = true)
        return payment
    }
}
