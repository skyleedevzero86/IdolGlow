package com.sleekydz86.idolglow.mypage.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationQueryService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationSummaryResponse
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import com.sleekydz86.idolglow.review.application.dto.ProductReviewResponse
import com.sleekydz86.idolglow.user.user.application.UserService
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mypage")
class MyPageController(
    private val reservationQueryService: ReservationQueryService,
    private val productReviewQueryService: ProductReviewQueryService,
    private val userService: UserService
) : MyPageApi {

    @GetMapping("/user")
    override fun getUser(@LoginUser userId: Long): ResponseEntity<GetUserLoginInfoResponse> =
        ResponseEntity.ok(userService.getUser(userId))

    @GetMapping("/bookings")
    override fun findBookings(@LoginUser userId: Long): ResponseEntity<List<ReservationSummaryResponse>> =
        ResponseEntity.ok(reservationQueryService.findReservationsByUser(userId))

    @GetMapping("/bookings/upcoming")
    override fun findUpcomingBookings(@LoginUser userId: Long): ResponseEntity<List<ReservationSummaryResponse>> =
        ResponseEntity.ok(reservationQueryService.findUpcomingReservationsByUser(userId))

    @GetMapping("/bookings/{reservationId}")
    override fun findBookingDetail(
        @LoginUser userId: Long,
        @PathVariable reservationId: Long
    ): ResponseEntity<ReservationSummaryResponse> =
        ResponseEntity.ok(reservationQueryService.findReservationDetail(reservationId, userId))

    @GetMapping("/reviews")
    override fun findMyReviews(@LoginUser userId: Long): ResponseEntity<List<ProductReviewResponse>> =
        ResponseEntity.ok(productReviewQueryService.findReviewsByUser(userId))
}
