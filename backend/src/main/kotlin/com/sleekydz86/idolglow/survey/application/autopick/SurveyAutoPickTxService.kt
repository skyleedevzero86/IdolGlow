package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.application.GlowPassBenefitPolicy
import com.sleekydz86.idolglow.payment.application.MembershipQuery
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickCandidate
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickFailReason
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickSetting
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickCandidateJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickProductJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickSettingJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Transactional
@Service
class SurveyAutoPickTxService(
    private val settingRepository: SurveyAutoPickSettingJpaRepository,
    private val productRepository: SurveyAutoPickProductJpaRepository,
    private val candidateRepository: SurveyAutoPickCandidateJpaRepository,
    private val benefitPolicy: GlowPassBenefitPolicy,
    private val membershipQuery: MembershipQuery,
    private val billingMethodRepository: TossBillingMethodJpaRepository,
    private val reservationCommandService: ReservationCommandService,
    private val monthlyCounter: SurveyAutoPickMonthlyCounter,
    private val notificationCommandService: NotificationCommandService,
) {
    fun createCandidates(now: LocalDateTime): Int {
        val settings = settingRepository.findAllByEnabledTrue()
        if (settings.isEmpty()) {
            return 0
        }
        val productsBySetting = productRepository.findAllBySettingIdIn(settings.map { it.id }).groupBy { it.settingId }
        val openingSlots = openingSlots(productsBySetting.values.flatten().map { it.productId }.toSet(), now)
        return settings.sumOf { setting ->
            val productIds = productsBySetting[setting.id].orEmpty().map { it.productId }.toSet()
            openingSlots.filter { it.product.id in productIds }.count { schedule(setting.userId, it) }
        }
    }

    fun claim(
        candidateId: Long,
        now: LocalDateTime,
    ): AutoPickClaim? {
        val candidate =
            candidateRepository.findByIdForUpdate(candidateId)?.takeIf { it.isDue(now) } ?: return null
        val blocked = blockReason(candidate, now)
        blocked?.let { candidate.fail(it) }
        return if (blocked == null) start(candidate, now) else null
    }

    private fun start(
        candidate: SurveyAutoPickCandidate,
        now: LocalDateTime,
    ): AutoPickClaim {
        candidate.startProcessing()
        val price = reservationCommandService.expectedTotalPrice(candidate.productId, candidate.userId, now)
        return AutoPickClaim(candidate.id, candidate.userId, candidate.productId, candidate.reservationSlotId, price)
    }

    fun book(
        candidateId: Long,
        reservationId: Long,
    ) {
        candidateRepository.findByIdForUpdate(candidateId)?.book(reservationId)
    }

    fun fail(
        candidateId: Long,
        reason: SurveyAutoPickFailReason,
    ) {
        candidateRepository.findByIdForUpdate(candidateId)?.fail(reason)
    }

    private fun openingSlots(
        productIds: Set<Long>,
        now: LocalDateTime,
    ): List<ReservationSlot> {
        if (productIds.isEmpty()) {
            return emptyList()
        }
        val latestStart = now.plus(benefitPolicy.memberOpenLead())
        val earliestStart = latestStart.minusMinutes(SurveyAutoPickCandidate.CHARGE_DELAY_MINUTES)
        return candidateRepository
            .findSlotsOfProducts(productIds, earliestStart.toLocalDate(), latestStart.toLocalDate())
            .filter { slot ->
                val opensAt = benefitPolicy.memberOpensAt(slot)
                opensAt.isAfter(now.minusMinutes(SurveyAutoPickCandidate.CHARGE_DELAY_MINUTES)) && !opensAt.isAfter(now)
            }
    }

    private fun schedule(
        userId: Long,
        slot: ReservationSlot,
    ): Boolean {
        if (candidateRepository.existsByUserIdAndReservationSlotId(userId, slot.id)) {
            return false
        }
        val opensAt = benefitPolicy.memberOpensAt(slot)
        val chargeAt = opensAt.plusMinutes(SurveyAutoPickCandidate.CHARGE_DELAY_MINUTES)
        candidateRepository.save(
            SurveyAutoPickCandidate(
                userId = userId,
                productId = slot.product.id,
                reservationSlotId = slot.id,
                memberOpenAt = opensAt,
                scheduledChargeAt = chargeAt,
            ),
        )
        notificationCommandService.create(
            userId = userId,
            type = NotificationType.AUTO_PICK_SCHEDULED,
            title = "자동 담기 결제 예정",
            message = "${slot.product.name} ${slot.reservationDate} ${slot.startTime} 슬롯을 $chargeAt 에 기본 카드로 예약합니다.",
            link = "/my-survey",
        )
        return true
    }

    private fun blockReason(
        candidate: SurveyAutoPickCandidate,
        now: LocalDateTime,
    ): SurveyAutoPickFailReason? {
        val setting = settingRepository.findByUserId(candidate.userId)
        return when {
            !isActiveTarget(setting, candidate) -> SurveyAutoPickFailReason.DISABLED
            !membershipQuery.isActive(candidate.userId, now) -> SurveyAutoPickFailReason.NOT_MEMBER
            billingMethodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(candidate.userId) == null ->
                SurveyAutoPickFailReason.NO_BILLING_METHOD
            monthlyCounter.bookedInMonth(candidate.userId, now) >= SurveyAutoPickMonthlyCounter.MONTHLY_LIMIT ->
                SurveyAutoPickFailReason.MONTHLY_LIMIT
            isOverBudget(candidate, setting!!.maxPrice, now) -> SurveyAutoPickFailReason.OVER_BUDGET
            else -> null
        }
    }

    private fun isActiveTarget(
        setting: SurveyAutoPickSetting?,
        candidate: SurveyAutoPickCandidate,
    ): Boolean =
        setting?.enabled == true &&
            productRepository.existsBySettingIdAndProductId(setting.id, candidate.productId)

    private fun isOverBudget(
        candidate: SurveyAutoPickCandidate,
        maxPrice: BigDecimal,
        now: LocalDateTime,
    ): Boolean = reservationCommandService.expectedTotalPrice(candidate.productId, candidate.userId, now) > maxPrice
}

data class AutoPickClaim(
    val candidateId: Long,
    val userId: Long,
    val productId: Long,
    val reservationSlotId: Long,
    val totalPrice: BigDecimal,
)
