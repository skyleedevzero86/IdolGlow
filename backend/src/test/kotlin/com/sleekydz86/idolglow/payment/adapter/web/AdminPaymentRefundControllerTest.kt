package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.infrastructure.exception.GlobalExceptionAdvice
import com.sleekydz86.idolglow.payment.application.AdminPaymentService
import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("관리자 결제 조회")
class AdminPaymentRefundControllerTest {
    private val adminPaymentService = mockk<AdminPaymentService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    AdminPaymentRefundController(
                        adminPaymentService = adminPaymentService,
                        paymentRefundService = mockk<PaymentRefundService>(relaxed = true),
                    ),
                ).setControllerAdvice(GlobalExceptionAdvice())
                .build()
    }

    @Test
    fun adminPaymentsFilterConfirming() {
        every {
            adminPaymentService.findPayments(PaymentStatus.CONFIRMING, null, null, 50)
        } returns emptyList()

        mockMvc
            .perform(get("/admin/payments").param("status", "CONFIRMING"))
            .andExpect(status().isOk)

        verify {
            adminPaymentService.findPayments(
                status = PaymentStatus.CONFIRMING,
                visitDate = null,
                productId = null,
                size = 50,
            )
        }
    }
}
