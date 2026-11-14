package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue
import com.sleekydz86.idolglow.newsletter.domain.Newsletter

interface SubscriptionDispatchRecorder {
    fun recordNewsletterDispatch(newsletter: Newsletter)

    fun recordIdolGlowIssueDispatch(issue: IdolGlowIssue)
}
