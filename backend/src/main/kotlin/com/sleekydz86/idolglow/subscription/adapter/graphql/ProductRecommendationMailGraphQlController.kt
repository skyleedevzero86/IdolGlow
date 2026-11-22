package com.sleekydz86.idolglow.subscription.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.subscription.adapter.web.request.SubscribeProductRecommendationMailRequest
import com.sleekydz86.idolglow.subscription.application.port.`in`.ProductRecommendationMailUseCase
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class ProductRecommendationMailGraphQlController(
    private val productRecommendationMailUseCase: ProductRecommendationMailUseCase,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {
    @QueryMapping
    fun myRecommendationMail(): RecommendationMailSettingGraphQlResponse =
        RecommendationMailSettingGraphQlResponse.from(
            productRecommendationMailUseCase.findMine(authenticatedUserIdResolver.resolveRequired()),
        )

    @MutationMapping
    fun subscribeRecommendationMail(
        @Argument @Valid input: SubscribeProductRecommendationMailRequest,
    ): RecommendationMailSettingGraphQlResponse =
        RecommendationMailSettingGraphQlResponse.from(
            productRecommendationMailUseCase.subscribe(
                input.toCommand(authenticatedUserIdResolver.resolveRequired()),
            ),
        )

    @MutationMapping
    fun unsubscribeRecommendationMail(): RecommendationMailSettingGraphQlResponse =
        RecommendationMailSettingGraphQlResponse.from(
            productRecommendationMailUseCase.unsubscribeMine(authenticatedUserIdResolver.resolveRequired()),
        )
}
