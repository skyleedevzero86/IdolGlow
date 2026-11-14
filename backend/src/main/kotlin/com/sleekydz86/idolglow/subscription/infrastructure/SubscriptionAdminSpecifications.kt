package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.dto.AdminDateRange
import com.sleekydz86.idolglow.subscription.application.dto.DispatchHistorySearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.DispatchRecipientSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.EmailSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.MailSendLogSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.normalizedKeyword
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime

object SubscriptionAdminSpecifications {
    const val LIKE_ESCAPE = '!'

    fun containsPattern(keyword: String?): String {
        val normalized = keyword.normalizedKeyword() ?: return "%"
        val escaped =
            normalized
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
        return "%$escaped%"
    }

    fun emailSubscribers(condition: EmailSubscriberSearchCondition): Specification<EmailSubscription> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            condition.keyword.normalizedKeyword()?.let {
                predicates += cb.like(cb.lower(root.get("email")), containsPattern(it), LIKE_ESCAPE)
            }
            condition.status?.let { predicates += cb.equal(root.get<Boolean>("active"), it.active) }
            condition.audience?.let { audience ->
                predicates +=
                    when (audience) {
                        SubscriptionAudience.NEWSLETTER -> cb.isTrue(root.get("subscribedNewsletters"))
                        SubscriptionAudience.WEBZINE_ISSUE -> cb.isTrue(root.get("subscribedIssues"))
                        SubscriptionAudience.PRODUCT_RECOMMENDATION -> cb.disjunction()
                    }
            }
            cb.and(*predicates.toTypedArray())
        }

    fun dispatches(condition: DispatchHistorySearchCondition): Specification<SubscriptionDispatchHistory> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            condition.keyword.normalizedKeyword()?.let {
                val pattern = containsPattern(it)
                predicates +=
                    cb.or(
                        cb.like(cb.lower(root.get("contentTitle")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("contentSlug")), pattern, LIKE_ESCAPE),
                    )
            }
            condition.contentType?.let { predicates += cb.equal(root.get<Any>("contentType"), it) }
            condition.status?.let { predicates += cb.equal(root.get<Any>("dispatchStatus"), it) }
            predicates += cb.within(root.get("dispatchedAt"), condition.range)
            cb.and(*predicates.toTypedArray())
        }

    fun dispatchRecipients(condition: DispatchRecipientSearchCondition): Specification<SubscriptionDispatchRecipient> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>(cb.equal(root.get<Long>("dispatchId"), condition.dispatchId))
            condition.keyword.normalizedKeyword()?.let {
                predicates += cb.like(cb.lower(root.get("email")), containsPattern(it), LIKE_ESCAPE)
            }
            condition.status?.let { predicates += cb.equal(root.get<Any>("status"), it) }
            cb.and(*predicates.toTypedArray())
        }

    fun mailLogs(condition: MailSendLogSearchCondition): Specification<MailSendLog> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            condition.keyword.normalizedKeyword()?.let {
                predicates += cb.like(cb.lower(root.get("recipientEmail")), containsPattern(it), LIKE_ESCAPE)
            }
            condition.kind?.let { predicates += cb.equal(root.get<Any>("mailKind"), it) }
            condition.status?.let { predicates += cb.equal(root.get<Any>("status"), it) }
            predicates += cb.within(root.get("createdAt"), condition.range)
            cb.and(*predicates.toTypedArray())
        }

    private fun CriteriaBuilder.within(
        path: Path<LocalDateTime>,
        range: AdminDateRange,
    ): Predicate {
        val bounds = mutableListOf<Predicate>()
        range.fromInclusive?.let { bounds += greaterThanOrEqualTo(path, it) }
        range.toExclusive?.let { bounds += lessThan(path, it) }
        return and(*bounds.toTypedArray())
    }
}
