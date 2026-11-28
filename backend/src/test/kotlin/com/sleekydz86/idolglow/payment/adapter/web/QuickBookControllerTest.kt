package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUserArgumentResolver
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.GlobalExceptionAdvice
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.payment.application.QuickBookService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCancelFacade
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("바로 예약 API")
class QuickBookControllerTest {
    private val quickBookService = mockk<QuickBookService>()
    private val cancelFacade = mockk<ReservationCancelFacade>()
    private val userIdResolver = mockk<AuthenticatedUserIdResolver>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        every { userIdResolver.resolveRequired() } returns USER_ID
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(QuickBookController(quickBookService, cancelFacade))
                .setControllerAdvice(GlobalExceptionAdvice())
                .setCustomArgumentResolvers(LoginUserArgumentResolver(userIdResolver))
                .build()
    }

    @DisplayName("R1 로그인하지 않으면 401 UNAUTHENTICATED 이고 예약하지 않는다")
    @Test
    fun unauthenticatedRejected() {
        // given
        every { userIdResolver.resolveRequired() } throws CustomException(AuthExceptionType.UNAUTHENTICATED)

        // when
        val result = postBook("""{"productId":1,"reservationSlotId":2,"totalPrice":9000}""")

        // then
        result.andExpect(status().isUnauthorized).andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
        verify(exactly = 0) { quickBookService.book(any()) }
    }

    @DisplayName("R1 슬롯 번호가 0 이면 400 이고 예약하지 않는다")
    @Test
    fun invalidSlotRejected() {
        // when
        val result = postBook("""{"productId":1,"reservationSlotId":0,"totalPrice":9000}""")

        // then
        result.andExpect(status().isBadRequest)
        verify(exactly = 0) { quickBookService.book(any()) }
    }

    @DisplayName("R8 요청은 바로 예약 경로로 서비스에 넘긴다")
    @Test
    fun passesQuickBookChannel() {
        // given
        every { quickBookService.book(any()) } throws CustomException(QuickBookExceptionType.BILLING_METHOD_REQUIRED)

        // when
        postBook("""{"productId":1,"reservationSlotId":2,"totalPrice":9000}""")

        // then
        verify {
            quickBookService.book(
                match { it.bookingChannel == ReservationBookingChannel.QUICK_BOOK && it.userId == USER_ID },
            )
        }
    }

    @DisplayName("바로 예약·바로 취소 오류는 정해진 상태값과 오류 코드로 내려간다")
    @ParameterizedTest(name = "{0}")
    @EnumSource(QuickBookExceptionType::class)
    fun errorsKeepContract(type: QuickBookExceptionType) {
        // given
        every { cancelFacade.quickCancelByUser(10L, USER_ID) } throws CustomException(type)

        // when
        val result = mockMvc.perform(post("/reservations/10/quick-cancel"))

        // then
        result
            .andExpect(status().`is`(type.httpStatusCode))
            .andExpect(jsonPath("$.errorCode").value(type.errorCode))
            .andExpect(jsonPath("$.message").value(type.message))
    }

    private fun postBook(body: String) =
        mockMvc.perform(post("/reservations/quick-book").contentType(MediaType.APPLICATION_JSON).content(body))

    private companion object {
        const val USER_ID = 7L
    }
}
