package com.sleekydz86.idolglow.productpackage.admin.graphql

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.global.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.graphql.toGraphQlLocalDate
import com.sleekydz86.idolglow.graphql.ProductCreatedGraphQlResponse
import com.sleekydz86.idolglow.productpackage.admin.application.AdminCatalogService
import com.sleekydz86.idolglow.productpackage.admin.application.AdminOperationsAnalyticsService
import com.sleekydz86.idolglow.productpackage.admin.application.AdminReservationService
import com.sleekydz86.idolglow.productpackage.option.application.OptionCommandService
import com.sleekydz86.idolglow.productpackage.option.graphql.OptionGraphQlResponse
import com.sleekydz86.idolglow.productpackage.option.ui.request.CreateOptionRequest
import com.sleekydz86.idolglow.productpackage.option.ui.request.toCommand
import com.sleekydz86.idolglow.productpackage.product.application.ProductCommandService
import com.sleekydz86.idolglow.productpackage.product.ui.request.CreateProductRequest
import com.sleekydz86.idolglow.productpackage.product.ui.request.toCommand
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.productpackage.admin.ui.request.CreateReservationSlotsRequest
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
class AdminGraphQlController(
    private val adminCatalogService: AdminCatalogService,
    private val adminReservationService: AdminReservationService,
    private val adminOperationsAnalyticsService: AdminOperationsAnalyticsService,
    private val productCommandService: ProductCommandService,
    private val optionCommandService: OptionCommandService,
) {

    @QueryMapping
    fun adminSubscriptionOverview(): AdminSubscriptionOverviewGraphQlResponse =
        AdminSubscriptionOverviewGraphQlResponse.from(AdminSubscriptionOverviewResponse())

    @QueryMapping
    fun adminReservationDashboard(
        @Argument fromDate: String?,
        @Argument toDate: String?,
        @Argument recentSize: Int?,
    ): AdminReservationDashboardGraphQlResponse =
        AdminReservationDashboardGraphQlResponse.from(
            adminReservationService.findDashboard(
                fromDate = fromDate?.takeIf { it.isNotBlank() }?.toGraphQlLocalDate("fromDate"),
                toDate = toDate?.takeIf { it.isNotBlank() }?.toGraphQlLocalDate("toDate"),
                recentSize = (recentSize ?: 10).coerceIn(1, 50)
            )
        )

    @QueryMapping
    fun adminReservations(
        @Argument status: ReservationStatus?,
        @Argument visitDate: String?,
        @Argument productId: String?,
        @Argument size: Int?,
    ): List<AdminReservationSummaryGraphQlResponse> =
        adminReservationService.findReservations(
            status = status,
            visitDate = visitDate?.takeIf { it.isNotBlank() }?.toGraphQlLocalDate("visitDate"),
            productId = productId?.takeIf { it.isNotBlank() }?.toGraphQlIdLong("productId"),
            size = (size ?: 50).coerceIn(1, 100)
        ).map(AdminReservationSummaryGraphQlResponse::from)

    @QueryMapping
    fun adminProductSlots(@Argument productId: String): List<AdminReservationSlotGraphQlResponse> =
        adminCatalogService.findSlots(productId.toGraphQlIdLong("productId"))
            .map(AdminReservationSlotGraphQlResponse::from)

    @QueryMapping
    fun adminMenuStats(): OperationsMenuStatsGraphQlResponse =
        OperationsMenuStatsGraphQlResponse.from(adminOperationsAnalyticsService.menuStats())

    @MutationMapping
    fun createOption(@Argument @Valid input: CreateOptionRequest): OptionGraphQlResponse =
        OptionGraphQlResponse.from(optionCommandService.createOption(input.toCommand()))

    @MutationMapping
    fun createProduct(@Argument @Valid input: CreateProductRequest): ProductCreatedGraphQlResponse =
        ProductCreatedGraphQlResponse.from(
            productCommandService.createProduct(input.toCommand()).id
        )

    @MutationMapping
    fun createAdminReservationSlots(
        @Argument productId: String,
        @Argument @Valid input: CreateReservationSlotsRequest,
    ): List<AdminReservationSlotGraphQlResponse> =
        adminCatalogService.createSlots(productId.toGraphQlIdLong("productId"), input)
            .map(AdminReservationSlotGraphQlResponse::from)

    @MutationMapping
    fun deleteAdminReservationSlot(@Argument slotId: String): Boolean {
        adminCatalogService.deleteSlot(slotId.toGraphQlIdLong("slotId"))
        return true
    }

    @MutationMapping
    fun deleteAdminProduct(@Argument productId: String): Boolean {
        adminCatalogService.deleteProduct(productId.toGraphQlIdLong("productId"))
        return true
    }

    @MutationMapping
    fun deleteAdminOption(@Argument optionId: String): Boolean {
        adminCatalogService.deleteOption(optionId.toGraphQlIdLong("optionId"))
        return true
    }

    @MutationMapping
    fun cancelAdminReservation(@Argument reservationId: String): AdminReservationSummaryGraphQlResponse =
        AdminReservationSummaryGraphQlResponse.from(
            adminReservationService.cancelReservation(reservationId.toGraphQlIdLong("reservationId"))
        )
}
