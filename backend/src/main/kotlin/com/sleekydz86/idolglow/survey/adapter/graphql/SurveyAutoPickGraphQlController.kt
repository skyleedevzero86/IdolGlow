package com.sleekydz86.idolglow.survey.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickCandidateResponse
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickResponse
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickSettingService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class SurveyAutoPickGraphQlController(
    private val settingService: SurveyAutoPickSettingService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun mySurveyAutoPick(): SurveyAutoPickGraphQlResponse =
        SurveyAutoPickGraphQlResponse.from(settingService.find(authenticatedUserIdResolver.resolveRequired()))
}

data class SurveyAutoPickGraphQlResponse(
    val enabled: Boolean,
    val productIds: List<String>,
    val maxPrice: String?,
    val monthlyLimit: Int,
    val bookedThisMonth: Int,
    val candidates: List<SurveyAutoPickCandidateGraphQlResponse>,
) {
    companion object {
        fun from(response: SurveyAutoPickResponse) =
            SurveyAutoPickGraphQlResponse(
                enabled = response.enabled,
                productIds = response.productIds.map { it.asGraphQlId() },
                maxPrice = response.maxPrice?.asGraphQlNumber(),
                monthlyLimit = response.monthlyLimit,
                bookedThisMonth = response.bookedThisMonth.toInt(),
                candidates = response.candidates.map(SurveyAutoPickCandidateGraphQlResponse::from),
            )
    }
}

data class SurveyAutoPickCandidateGraphQlResponse(
    val id: String,
    val productId: String,
    val reservationSlotId: String,
    val status: String,
    val memberOpenAt: String,
    val scheduledChargeAt: String,
    val failReason: String?,
    val reservationId: String?,
) {
    companion object {
        fun from(candidate: SurveyAutoPickCandidateResponse) =
            SurveyAutoPickCandidateGraphQlResponse(
                id = candidate.id.asGraphQlId(),
                productId = candidate.productId.asGraphQlId(),
                reservationSlotId = candidate.reservationSlotId.asGraphQlId(),
                status = candidate.status.name,
                memberOpenAt = candidate.memberOpenAt.toString(),
                scheduledChargeAt = candidate.scheduledChargeAt.toString(),
                failReason = candidate.failReason?.name,
                reservationId = candidate.reservationId?.asGraphQlId(),
            )
    }
}
