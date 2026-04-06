package com.sleekydz86.idolglow.subscription.ui

import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionPublicUseCase
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionRegistrationResponse
import com.sleekydz86.idolglow.subscription.ui.request.RegisterSubscriptionRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "Subscription", description = "Public API for Idol Glow email subscription registration")
@RestController
@RequestMapping("/subscriptions")
class SubscriptionController(
    private val subscriptionPublicUseCase: SubscriptionPublicUseCase,
) {

    @Operation(summary = "Register subscription")
    @PostMapping
    fun subscribe(
        @Valid @RequestBody request: RegisterSubscriptionRequest,
    ): ResponseEntity<SubscriptionRegistrationResponse> {
        val created = subscriptionPublicUseCase.subscribe(
            RegisterSubscriptionCommand(
                email = request.email,
                subscribeNewsletters = request.subscribeNewsletters,
                subscribeIssues = request.subscribeIssues,
                source = "WEB_MODAL",
            )
        )

        return ResponseEntity
            .created(URI.create("/subscriptions/${created.id}"))
            .body(created)
    }
}
