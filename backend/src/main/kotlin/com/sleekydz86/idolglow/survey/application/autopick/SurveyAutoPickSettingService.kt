package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.AutoPickExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.payment.application.MembershipQuery
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickProduct
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickSetting
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickCandidateJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickProductJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickSettingJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
@Service
class SurveyAutoPickSettingService(
    private val settingRepository: SurveyAutoPickSettingJpaRepository,
    private val productRepository: SurveyAutoPickProductJpaRepository,
    private val candidateRepository: SurveyAutoPickCandidateJpaRepository,
    private val catalog: ProductCommandRepository,
    private val membershipQuery: MembershipQuery,
    private val billingMethodRepository: TossBillingMethodJpaRepository,
    private val monthlyCounter: SurveyAutoPickMonthlyCounter,
) {
    @Transactional(readOnly = true)
    fun find(
        userId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): SurveyAutoPickResponse {
        val setting = settingRepository.findByUserId(userId)
        return SurveyAutoPickResponse(
            enabled = setting?.enabled ?: false,
            productIds = setting?.let { productIdsOf(it.id) }.orEmpty(),
            maxPrice = setting?.maxPrice,
            monthlyLimit = SurveyAutoPickMonthlyCounter.MONTHLY_LIMIT,
            bookedThisMonth = monthlyCounter.bookedInMonth(userId, now),
            candidates =
                candidateRepository
                    .findTop20ByUserIdOrderByScheduledChargeAtDesc(userId)
                    .map(SurveyAutoPickCandidateResponse::from),
        )
    }

    fun update(
        userId: Long,
        command: SurveyAutoPickCommand,
        now: LocalDateTime = LocalDateTime.now(),
    ): SurveyAutoPickResponse {
        val productIds = validProductIds(command)
        if (command.enabled) {
            requireReady(userId, now)
        }
        val setting =
            settingRepository.findByUserId(userId)
                ?: settingRepository.save(SurveyAutoPickSetting(userId = userId, maxPrice = command.maxPrice))
        setting.update(command.enabled, command.maxPrice)
        productRepository.deleteAllBySettingId(setting.id)
        productRepository.saveAll(productIds.map { SurveyAutoPickProduct(settingId = setting.id, productId = it) })
        return find(userId, now)
    }

    fun skip(
        userId: Long,
        candidateId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): SurveyAutoPickCandidateResponse {
        val candidate =
            candidateRepository.findByIdForUpdate(candidateId)?.takeIf { it.userId == userId }
                ?: throw CustomException(AutoPickExceptionType.CANDIDATE_NOT_FOUND)
        if (!candidate.canSkip(now)) {
            throw CustomException(AutoPickExceptionType.SKIP_CLOSED)
        }
        candidate.skip(now)
        return SurveyAutoPickCandidateResponse.from(candidate)
    }

    private fun validProductIds(command: SurveyAutoPickCommand): List<Long> {
        val productIds = command.productIds.distinct()
        val error =
            when {
                productIds.size !in 1..MAX_PRODUCTS -> AutoPickExceptionType.INVALID_PRODUCTS
                command.maxPrice.signum() < 0 -> AutoPickExceptionType.INVALID_MAX_PRICE
                productIds.any { !catalog.existsById(it) } -> AutoPickExceptionType.PRODUCT_NOT_FOUND
                else -> null
            }
        if (error != null) {
            throw CustomException(error)
        }
        return productIds
    }

    private fun requireReady(
        userId: Long,
        now: LocalDateTime,
    ) {
        if (!membershipQuery.isActive(userId, now)) {
            throw CustomException(AutoPickExceptionType.MEMBERSHIP_REQUIRED)
        }
        billingMethodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(userId)
            ?: throw CustomException(QuickBookExceptionType.BILLING_METHOD_REQUIRED)
    }

    private fun productIdsOf(settingId: Long): List<Long> =
        productRepository.findAllBySettingId(settingId).map { it.productId }.sorted()

    private companion object {
        const val MAX_PRODUCTS = 10
    }
}
