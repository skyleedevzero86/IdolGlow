package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/subscriptions")
class AdminSubscriptionController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun overview(): ResponseEntity<AdminSubscriptionOverviewResponse> =
        ResponseEntity.ok(AdminSubscriptionOverviewResponse())
}
