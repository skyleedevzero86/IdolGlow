package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionRegistrationResponse
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue

interface SubscriptionPublicUseCase {
    fun subscribe(command: RegisterSubscriptionCommand): SubscriptionRegistrationResponse
}

interface SubscriptionAdminUseCase {
    fun findOverview(
        subscriberPage: Int,
        subscriberSize: Int,
        dispatchPage: Int,
        dispatchSize: Int,
    ): AdminSubscriptionOverviewResponse
}

interface SubscriptionDispatchRecorder {
    fun recordNewsletterDispatch(newsletter: Newsletter)
    fun recordWebzineIssueDispatch(issue: WebzineIssue)
}
