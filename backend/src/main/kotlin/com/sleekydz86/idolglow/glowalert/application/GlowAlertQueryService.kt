package com.sleekydz86.idolglow.glowalert.application

import com.sleekydz86.idolglow.glowalert.application.dto.GlowAlertCategoryResponse
import com.sleekydz86.idolglow.glowalert.application.dto.GlowAlertItemResponse
import com.sleekydz86.idolglow.glowalert.application.dto.GlowAlertPageResponse
import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Service
@Transactional
class GlowAlertQueryService(
    private val notificationRepository: NotificationRepository,
) {

    fun findAlerts(
        page: Int,
        size: Int,
        status: String,
        category: String,
        keyword: String,
        userId: Long? = null,
    ): GlowAlertPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 20)
        val resolvedStatus = status.normalizeStatus()
        val resolvedCategory = category.normalizeCategory()
        val normalizedKeyword = keyword.trim().lowercase()
        val now = LocalDateTime.now()

        if (userId != null) {
            return findDatabaseAlerts(
                userId = userId,
                page = resolvedPage,
                size = resolvedSize,
                status = resolvedStatus,
                category = resolvedCategory,
                keyword = normalizedKeyword,
                now = now,
            )
        }

        val statusItems = alertSeeds
            .filter { it.isVisibleAt(now) }
            .filter { it.matchesKeyword(normalizedKeyword) }
            .filter { it.matchesStatus(resolvedStatus) }
        val filteredItems = statusItems
            .filter { resolvedCategory == ALL_CATEGORY || it.category == resolvedCategory }

        val fromIndex = ((resolvedPage - 1) * resolvedSize).coerceAtMost(filteredItems.size)
        val toIndex = (fromIndex + resolvedSize).coerceAtMost(filteredItems.size)
        val totalElements = filteredItems.size.toLong()
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + resolvedSize - 1) / resolvedSize).toInt()

        return GlowAlertPageResponse(
            items = filteredItems.subList(fromIndex, toIndex).map { it.toResponse(now) },
            categories = buildCategories(statusItems.map { it.category }),
            page = resolvedPage,
            size = resolvedSize,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = toIndex < filteredItems.size,
            tab = resolvedStatus,
            activeCategory = resolvedCategory,
        )
    }

    private fun findDatabaseAlerts(
        userId: Long,
        page: Int,
        size: Int,
        status: String,
        category: String,
        keyword: String,
        now: LocalDateTime,
    ): GlowAlertPageResponse {
        val statusItems = notificationRepository.findVisibleByUserId(userId, now.minusMonths(1))
            .filter { it.matchesKeyword(keyword) }
            .filter { it.matchesStatus(status) }
        val filteredItems = statusItems
            .filter { category == ALL_CATEGORY || it.categoryId() == category }
        val fromIndex = ((page - 1) * size).coerceAtMost(filteredItems.size)
        val toIndex = (fromIndex + size).coerceAtMost(filteredItems.size)
        val totalElements = filteredItems.size.toLong()
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()

        return GlowAlertPageResponse(
            items = filteredItems.subList(fromIndex, toIndex).map { it.toGlowAlertResponse(now) },
            categories = buildCategories(statusItems.map { it.categoryId() }),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = toIndex < filteredItems.size,
            tab = status,
            activeCategory = category,
        )
    }

    @Transactional(readOnly = true)
    fun countUnread(userId: Long? = null): Long {
        val now = LocalDateTime.now()
        if (userId != null) {
            return notificationRepository.countUnreadVisibleByUserId(userId, now.minusMonths(1))
        }
        return alertSeeds
            .asSequence()
            .filter { it.isVisibleAt(now) }
            .filter { it.matchesStatus("unread") }
            .count()
            .toLong()
    }

    fun markRead(alertId: Long, userId: Long? = null): Boolean {
        if (userId != null) {
            val notification = notificationRepository.findById(alertId) ?: return false
            if (notification.userId != userId) {
                return false
            }
            notification.markRead(LocalDateTime.now())
            notificationRepository.save(notification)
            return true
        }

        val exists = alertSeeds.any { it.id == alertId }
        if (exists) {
            readAlertIds.add(alertId)
        }
        return exists
    }

    private fun buildCategories(categoryIds: List<String>): List<GlowAlertCategoryResponse> {
        val categoryCounts = categoryIds.groupingBy { it }.eachCount()
        return listOf(
            GlowAlertCategoryResponse(ALL_CATEGORY, "전체", categoryIds.size.toLong()),
            GlowAlertCategoryResponse("verification", "회원인증", (categoryCounts["verification"] ?: 0).toLong()),
            GlowAlertCategoryResponse("activity", "활동소식", (categoryCounts["activity"] ?: 0).toLong()),
            GlowAlertCategoryResponse("finance", "금융·자산", (categoryCounts["finance"] ?: 0).toLong()),
        )
    }

    private fun String.normalizeStatus(): String =
        when (trim().lowercase()) {
            "read" -> "read"
            else -> "unread"
        }

    private fun String.normalizeCategory(): String {
        val normalized = trim().lowercase()
        return if (normalized in allowedCategories) normalized else ALL_CATEGORY
    }

    companion object {
        private const val ALL_CATEGORY = "all"
        private val allowedCategories = setOf(ALL_CATEGORY, "verification", "activity", "finance")
        internal val readAlertIds = ConcurrentHashMap.newKeySet<Long>()

        private val alertSeeds = listOf(
            GlowAlertSeed(
                id = 1,
                tab = "alert",
                category = "verification",
                categoryLabel = "회원인증",
                senderName = "라온시큐어/예술경영지원...",
                channelLabel = null,
                message = "네이버 인증서로 안전하게 인증되었어요. 사용 이력을 확인해보세요.",
                receivedAt = "2026-04-28T01:22:00",
                iconText = "CM",
                iconTone = "#fff1f6",
                unread = true,
            ),
            GlowAlertSeed(
                id = 2,
                tab = "alert",
                category = "activity",
                categoryLabel = "활동소식",
                senderName = "야생뉴",
                channelLabel = "블로그",
                message = "네 로드3 부스터라고 강화에요 no3 강화일까요?",
                receivedAt = "2026-04-26T14:12:00",
                iconText = "야",
                iconTone = "#f2e7e2",
                unread = false,
            ),
            GlowAlertSeed(
                id = 3,
                tab = "alert",
                category = "activity",
                categoryLabel = "활동소식",
                senderName = "마왕군간부",
                channelLabel = "블로그",
                message = "게다가 신장에 녹스의 미드나잇새도우 폼이 나오네요 기대돼요",
                receivedAt = "2026-04-25T18:34:00",
                iconText = "마",
                iconTone = "#ece6dc",
                unread = false,
            ),
            GlowAlertSeed(
                id = 4,
                tab = "alert",
                category = "finance",
                categoryLabel = "금융·자산",
                senderName = "신용점수",
                channelLabel = null,
                message = "이번 달 내 신용점수의 예상 최저금리가 변경되었어요. 대출리포트에서 확인해보세요.",
                receivedAt = "2026-04-22T09:05:00",
                iconText = "W",
                iconTone = "#dcfce7",
                unread = false,
            ),
            GlowAlertSeed(
                id = 5,
                tab = "alert",
                category = "verification",
                categoryLabel = "회원인증",
                senderName = "IDOL GLOW",
                channelLabel = "보안",
                message = "새 기기에서 로그인했어요. 본인이 맞는지 로그인 기록을 확인해 주세요.",
                receivedAt = "2026-04-21T20:16:00",
                iconText = "IG",
                iconTone = "#eef2ff",
                unread = true,
            ),
            GlowAlertSeed(
                id = 6,
                tab = "alert",
                category = "activity",
                categoryLabel = "활동소식",
                senderName = "Glow 큐레이션",
                channelLabel = "아카이브",
                message = "관심 아티스트의 신규 공연 아카이브가 업데이트되었습니다.",
                receivedAt = "2026-04-20T11:18:00",
                iconText = "G",
                iconTone = "#fef3c7",
                unread = false,
            ),
            GlowAlertSeed(
                id = 7,
                tab = "alert",
                category = "finance",
                categoryLabel = "금융·자산",
                senderName = "결제알림",
                channelLabel = "예약",
                message = "예매 결제 영수증이 발급되었어요. 마이페이지에서 확인할 수 있습니다.",
                receivedAt = "2026-04-18T16:40:00",
                iconText = "P",
                iconTone = "#e0f2fe",
                unread = false,
            ),
            GlowAlertSeed(
                id = 8,
                tab = "alert",
                category = "activity",
                categoryLabel = "활동소식",
                senderName = "공지",
                channelLabel = "뉴스",
                message = "이번 주 인기 아티클과 이벤트를 모아봤어요.",
                receivedAt = "2026-04-17T08:00:00",
                iconText = "N",
                iconTone = "#f1f5f9",
                unread = false,
            ),
            GlowAlertSeed(
                id = 9,
                tab = "conversation",
                category = "activity",
                categoryLabel = "활동소식",
                senderName = "Glow 매니저",
                channelLabel = "대화",
                message = "문의하신 전시 예약 변경 가능 시간을 안내드렸습니다.",
                receivedAt = "2026-04-28T10:30:00",
                iconText = "GM",
                iconTone = "#ede9fe",
                unread = true,
            ),
            GlowAlertSeed(
                id = 10,
                tab = "conversation",
                category = "verification",
                categoryLabel = "회원인증",
                senderName = "고객센터",
                channelLabel = "인증",
                message = "본인 확인 절차가 완료되어 답변을 이어서 확인할 수 있어요.",
                receivedAt = "2026-04-27T15:05:00",
                iconText = "CS",
                iconTone = "#fee2e2",
                unread = false,
            ),
            GlowAlertSeed(
                id = 11,
                tab = "conversation",
                category = "finance",
                categoryLabel = "금융·자산",
                senderName = "결제상담",
                channelLabel = "대화",
                message = "환불 접수 상태가 업데이트되었습니다.",
                receivedAt = "2026-04-24T12:12:00",
                iconText = "₩",
                iconTone = "#dcfce7",
                unread = false,
            ),
            GlowAlertSeed(
                id = 12,
                tab = "alert",
                category = "activity",
                categoryLabel = "활동소식",
                senderName = "만료 테스트",
                channelLabel = "아카이브",
                message = "한 달이 지난 알림은 데이터가 남아 있어도 목록에 노출되지 않습니다.",
                receivedAt = "2026-03-20T10:00:00",
                iconText = "OLD",
                iconTone = "#e5e7eb",
                unread = true,
            ),
        )
    }
}

private data class GlowAlertSeed(
    val id: Long,
    val tab: String,
    val category: String,
    val categoryLabel: String,
    val senderName: String,
    val channelLabel: String?,
    val message: String,
    val receivedAt: String,
    val iconText: String,
    val iconTone: String,
    val unread: Boolean,
) {
    private val receivedAtDateTime: LocalDateTime
        get() = LocalDateTime.parse(receivedAt)

    private fun effectiveUnread(): Boolean = unread && id !in GlowAlertQueryService.readAlertIds

    fun isVisibleAt(now: LocalDateTime): Boolean =
        !receivedAtDateTime.isBefore(now.minusMonths(1))

    fun matchesStatus(status: String): Boolean =
        when (status) {
            "read" -> !effectiveUnread()
            else -> effectiveUnread()
        }

    fun matchesKeyword(keyword: String): Boolean {
        if (keyword.isBlank()) {
            return true
        }
        return listOfNotNull(senderName, channelLabel, categoryLabel, message)
            .any { it.lowercase().contains(keyword) }
    }

    fun toResponse(now: LocalDateTime): GlowAlertItemResponse =
        GlowAlertItemResponse(
            id = id,
            tab = tab,
            category = category,
            categoryLabel = categoryLabel,
            senderName = senderName,
            channelLabel = channelLabel,
            message = message,
            receivedAt = receivedAt,
            receivedAtLabel = receivedAtDateTime.toRelativeLabel(now),
            iconText = iconText,
            iconTone = iconTone,
            unread = effectiveUnread(),
        )
}

private fun Notification.matchesStatus(status: String): Boolean =
    when (status) {
        "read" -> readAt != null
        else -> readAt == null
    }

private fun Notification.matchesKeyword(keyword: String): Boolean {
    if (keyword.isBlank()) {
        return true
    }
    return listOfNotNull(title, message, link, type.name, type.categoryLabel(), type.channelLabel())
        .any { it.lowercase().contains(keyword) }
}

private fun Notification.categoryId(): String =
    type.categoryId()

private fun Notification.toGlowAlertResponse(now: LocalDateTime): GlowAlertItemResponse {
    val created = createdAt ?: now
    return GlowAlertItemResponse(
        id = id,
        tab = "alert",
        category = type.categoryId(),
        categoryLabel = type.categoryLabel(),
        senderName = title,
        channelLabel = type.channelLabel(),
        message = message,
        receivedAt = created.toString(),
        receivedAtLabel = created.toRelativeLabel(now),
        iconText = type.iconText(),
        iconTone = type.iconTone(),
        unread = readAt == null,
    )
}

private fun NotificationType.categoryId(): String =
    when (this) {
        NotificationType.PAYMENT_FAILED,
        NotificationType.PAYMENT_EXPIRED -> "finance"
        NotificationType.RESERVATION_CONFIRMED,
        NotificationType.RESERVATION_CANCELED,
        NotificationType.RESERVATION_SLOT_AVAILABLE,
        NotificationType.RESERVATION_EXPIRING_SOON -> "activity"
    }

private fun NotificationType.categoryLabel(): String =
    when (categoryId()) {
        "finance" -> "금융·자산"
        "verification" -> "회원인증"
        else -> "활동소식"
    }

private fun NotificationType.channelLabel(): String =
    when (this) {
        NotificationType.PAYMENT_FAILED,
        NotificationType.PAYMENT_EXPIRED -> "결제"
        NotificationType.RESERVATION_CONFIRMED,
        NotificationType.RESERVATION_CANCELED,
        NotificationType.RESERVATION_SLOT_AVAILABLE,
        NotificationType.RESERVATION_EXPIRING_SOON -> "예약"
    }

private fun NotificationType.iconText(): String =
    when (this.categoryId()) {
        "finance" -> "₩"
        else -> "G"
    }

private fun NotificationType.iconTone(): String =
    when (this.categoryId()) {
        "finance" -> "#dcfce7"
        else -> "#fef3c7"
    }

private fun LocalDateTime.toRelativeLabel(now: LocalDateTime): String {
    if (isAfter(now)) {
        return "방금 전"
    }

    val minutes = Duration.between(this, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분전"
        minutes < 60 * 24 -> "${minutes / 60}시간전"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}일전"
        else -> "${ChronoUnit.WEEKS.between(this.toLocalDate(), now.toLocalDate()).coerceAtLeast(1)}주전"
    }
}
