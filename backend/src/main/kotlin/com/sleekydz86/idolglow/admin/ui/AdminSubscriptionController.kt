package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionAdminUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Admin subscription",
    description = "Admin API for subscription members and dispatch history",
)
@RestController
@RequestMapping("/admin/subscriptions")
class AdminSubscriptionController(
    private val subscriptionAdminUseCase: SubscriptionAdminUseCase,
) {

    @Operation(
        summary = "Get subscription overview",
        description = "Returns subscriber statistics and recent dispatch history for the admin subscriptions page",
    )
    @ApiResponse(responseCode = "200", description = "Subscription overview fetched successfully")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun overview(
        @RequestParam(defaultValue = "1") subscriberPage: Int,
        @RequestParam(defaultValue = "10") subscriberSize: Int,
        @RequestParam(defaultValue = "1") dispatchPage: Int,
        @RequestParam(defaultValue = "10") dispatchSize: Int,
    ): ResponseEntity<AdminSubscriptionOverviewResponse> =
        ResponseEntity.ok(
            subscriptionAdminUseCase.findOverview(
                subscriberPage = subscriberPage,
                subscriberSize = subscriberSize,
                dispatchPage = dispatchPage,
                dispatchSize = dispatchSize,
            )
        )
}
