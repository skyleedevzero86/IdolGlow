package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUserArgumentResolver
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.GlobalExceptionAdvice
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.BillingPlanChangeExceptionType
import com.sleekydz86.idolglow.payment.application.BillingPlanChangeService
import com.sleekydz86.idolglow.payment.application.PlanChangeCommand
import com.sleekydz86.idolglow.payment.application.dto.PlanChangePreviewResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@DisplayName("이용권 변경 API")
class TossBillingPlanChangeControllerTest {
    private val planChangeService = mockk<BillingPlanChangeService>()
    private val userIdResolver = mockk<AuthenticatedUserIdResolver>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        every { userIdResolver.resolveRequired() } returns USER_ID
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    TossBillingPlanChangeController(billingPlanChangeService = planChangeService),
                ).setControllerAdvice(GlobalExceptionAdvice())
                .setCustomArgumentResolvers(LoginUserArgumentResolver(userIdResolver))
                .build()
    }

    @DisplayName("미리보기는 로그인 회원의 정산 금액을 200 으로 돌려준다")
    @Test
    fun previewReturnsQuote() {
        // given
        every { planChangeService.preview(USER_ID, 12, any()) } returns preview()

        // when
        val result = mockMvc.perform(get(PREVIEW_PATH).param("cycleMonths", "12"))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cardAmount").value(33_252))
            .andExpect(jsonPath("$.remainingValue").value(13_748))
    }

    @DisplayName("로그인하지 않으면 401 UNAUTHENTICATED 이고 서비스를 부르지 않는다")
    @Test
    fun unauthenticatedRejected() {
        // given
        every { userIdResolver.resolveRequired() } throws CustomException(AuthExceptionType.UNAUTHENTICATED)

        // when
        val result = mockMvc.perform(get(PREVIEW_PATH).param("cycleMonths", "12"))

        // then
        result
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
        verify(exactly = 0) { planChangeService.preview(any(), any(), any()) }
    }

    @DisplayName("변경 오류는 정해진 상태값과 오류 코드로 내려간다")
    @ParameterizedTest(name = "{0}")
    @EnumSource(BillingPlanChangeExceptionType::class)
    fun changeErrorsKeepContract(type: BillingPlanChangeExceptionType) {
        // given
        every { planChangeService.change(USER_ID, any(), any()) } throws CustomException(type)

        // when
        val result = postChange("""{"cycleMonths":12,"expectedCardAmount":33252,"expectedCreditAdded":0}""")

        // then
        result
            .andExpect(status().`is`(type.httpStatusCode))
            .andExpect(jsonPath("$.errorCode").value(type.errorCode))
            .andExpect(jsonPath("$.message").value(type.message))
    }

    @DisplayName("기대 카드 금액이 음수면 400 이고 서비스를 부르지 않는다")
    @Test
    fun negativeExpectedAmountRejected() {
        // when
        val result = postChange("""{"cycleMonths":12,"expectedCardAmount":-1,"expectedCreditAdded":0}""")

        // then
        result.andExpect(status().isBadRequest)
        verify(exactly = 0) { planChangeService.change(any(), any(), any()) }
    }

    @DisplayName("변경 요청 본문을 그대로 명령으로 넘긴다")
    @Test
    fun changePassesCommand() {
        // given
        every { planChangeService.change(USER_ID, any(), any()) } throws
            CustomException(BillingPlanChangeExceptionType.AMOUNT_CHANGED)

        // when
        postChange("""{"cycleMonths":3,"expectedCardAmount":0,"expectedCreditAdded":32971}""")

        // then
        verify { planChangeService.change(USER_ID, PlanChangeCommand(3, 0L, 32_971L), any()) }
    }

    private fun postChange(body: String) =
        mockMvc.perform(
            post("/payments/toss/billing/subscriptions/plan-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

    private fun preview() =
        PlanChangePreviewResponse(
            currentCycleMonths = 3,
            newCycleMonths = 12,
            periodDays = 92,
            remainingDays = 91,
            remainingValue = 13_748L,
            newAmount = 47_000L,
            creditBalance = 0L,
            creditUsed = 0L,
            cardAmount = 33_252L,
            creditAdded = 0L,
            nextBillingAt = LocalDateTime.of(2027, 11, 28, 10, 0),
        )

    companion object {
        private const val USER_ID = 7L
        private const val PREVIEW_PATH = "/payments/toss/billing/subscriptions/plan-change/preview"
    }
}
