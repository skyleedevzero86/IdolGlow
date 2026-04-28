package com.sleekydz86.idolglow.payment.graphql

import com.sleekydz86.idolglow.global.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.graphql.toGraphQlLocalDate
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.payment.application.AdminPaymentService
import com.sleekydz86.idolglow.payment.application.MyPagePaymentService
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class PaymentManagementGraphQlController(
    private val adminPaymentService: AdminPaymentService,
    private val myPagePaymentService: MyPagePaymentService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun adminPayments(
        @Argument status: PaymentStatus?,
        @Argument visitDate: String?,
        @Argument productId: String?,
        @Argument size: Int?,
    ): List<AdminPaymentSummaryGraphQlResponse> =
        adminPaymentService.findPayments(
            status = status,
            visitDate = visitDate?.takeIf { it.isNotBlank() }?.toGraphQlLocalDate("visitDate"),
            productId = productId?.takeIf { it.isNotBlank() }?.toGraphQlIdLong("productId"),
            size = (size ?: 50).coerceIn(1, 200),
        ).map(AdminPaymentSummaryGraphQlResponse::from)

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun adminPaymentOverview(
        @Argument status: PaymentStatus?,
        @Argument visitDate: String?,
        @Argument productId: String?,
    ): AdminPaymentOverviewGraphQlResponse =
        AdminPaymentOverviewGraphQlResponse.from(
            adminPaymentService.overview(
                status = status,
                visitDate = visitDate?.takeIf { it.isNotBlank() }?.toGraphQlLocalDate("visitDate"),
                productId = productId?.takeIf { it.isNotBlank() }?.toGraphQlIdLong("productId"),
            )
        )

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun adminPaymentCharts(
        @Argument status: PaymentStatus?,
        @Argument visitDate: String?,
        @Argument productId: String?,
    ): AdminPaymentChartsGraphQlResponse =
        AdminPaymentChartsGraphQlResponse.from(
            adminPaymentService.charts(
                status = status,
                visitDate = visitDate?.takeIf { it.isNotBlank() }?.toGraphQlLocalDate("visitDate"),
                productId = productId?.takeIf { it.isNotBlank() }?.toGraphQlIdLong("productId"),
            )
        )

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun adminPaymentDetail(@Argument paymentId: String): AdminPaymentDetailGraphQlResponse =
        AdminPaymentDetailGraphQlResponse.from(
            adminPaymentService.findPaymentDetail(paymentId.toGraphQlIdLong("paymentId"))
        )

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myPayments(@Argument size: Int?): List<MyPaymentSummaryGraphQlResponse> =
        myPagePaymentService.findPayments(
            userId = authenticatedUserIdResolver.resolveRequired(),
            size = (size ?: 100).coerceIn(1, 200),
        ).map(MyPaymentSummaryGraphQlResponse::from)

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myPaymentDetail(@Argument paymentId: String): MyPaymentSummaryGraphQlResponse =
        MyPaymentSummaryGraphQlResponse.from(
            myPagePaymentService.findPayment(
                userId = authenticatedUserIdResolver.resolveRequired(),
                paymentId = paymentId.toGraphQlIdLong("paymentId"),
            )
        )

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun cancelMyPayment(
        @Argument paymentId: String,
        @Argument reason: String?,
    ): MyPaymentSummaryGraphQlResponse =
        MyPaymentSummaryGraphQlResponse.from(
            myPagePaymentService.cancelPayment(
                userId = authenticatedUserIdResolver.resolveRequired(),
                paymentId = paymentId.toGraphQlIdLong("paymentId"),
                reason = reason,
            )
        )
}
