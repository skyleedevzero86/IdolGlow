package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue

interface SubscriptionDispatchRecorder {
    fun recordNewsletterDispatch(newsletter: Newsletter)
    fun recordWebzineIssueDispatch(issue: WebzineIssue)
}
