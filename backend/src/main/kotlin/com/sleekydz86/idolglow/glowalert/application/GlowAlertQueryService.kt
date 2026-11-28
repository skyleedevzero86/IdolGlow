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
        userId: Long,
    ): GlowAlertPageResponse =
        findDatabaseAlerts(
            userId = userId,
            page = page.coerceAtLeast(1),
            size = size.coerceIn(1, 20),
            status = status.normalizeStatus(),
            category = category.normalizeCategory(),
            keyword = keyword.trim().lowercase(),
            now = LocalDateTime.now(),
        )

    private fun findDatabaseAlerts(
        userId: Long,
        page: Int,
        size: Int,
        status: String,
        category: String,
        keyword: String,
        now: LocalDateTime,
    ): GlowAlertPageResponse {
        val statusItems =
            notificationRepository
                .findVisibleByUserId(userId, now.minusMonths(1))
                .filter { it.matchesKeyword(keyword) }
                .filter { it.matchesStatus(status) }
        val filteredItems =
            statusItems
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
    fun countUnread(userId: Long): Long =
        notificationRepository.countUnreadVisibleByUserId(userId, LocalDateTime.now().minusMonths(1))

    fun markRead(
        alertId: Long,
        userId: Long,
    ): Boolean {
        val notification =
            notificationRepository.findById(alertId)?.takeIf { it.userId == userId } ?: return false
        notification.markRead(LocalDateTime.now())
        notificationRepository.save(notification)
        return true
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
    }
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

private fun Notification.categoryId(): String = type.categoryId()

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
        NotificationType.PAYMENT_EXPIRED,
        NotificationType.ADMIN_PAYMENT_COMPLETED,
        NotificationType.ADMIN_PAYMENT_ISSUE,
        -> "finance"
        NotificationType.RESERVATION_CONFIRMED,
        NotificationType.RESERVATION_CANCELED,
        NotificationType.RESERVATION_SLOT_AVAILABLE,
        NotificationType.RESERVATION_EXPIRING_SOON,
        NotificationType.AUTO_PICK_SCHEDULED,
        NotificationType.COMMENT_ON_MY_POST,
        NotificationType.REPLY_TO_MY_COMMENT,
        NotificationType.ADMIN_POST_REPORTED,
        NotificationType.ADMIN_POST_AUTO_BLINDED,
        NotificationType.EVENT_INTEREST_CHANGED,
        NotificationType.LUCKY_DRAW_WON,
        -> "activity"
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
        NotificationType.PAYMENT_EXPIRED,
        -> "결제"
        NotificationType.RESERVATION_CONFIRMED,
        NotificationType.RESERVATION_CANCELED,
        NotificationType.RESERVATION_SLOT_AVAILABLE,
        NotificationType.RESERVATION_EXPIRING_SOON,
        NotificationType.AUTO_PICK_SCHEDULED,
        -> "예약"
        NotificationType.COMMENT_ON_MY_POST,
        NotificationType.REPLY_TO_MY_COMMENT,
        -> "커뮤니티"
        NotificationType.EVENT_INTEREST_CHANGED -> "관심 공연"
        NotificationType.LUCKY_DRAW_WON -> "응모"
        NotificationType.ADMIN_POST_REPORTED,
        NotificationType.ADMIN_POST_AUTO_BLINDED,
        NotificationType.ADMIN_PAYMENT_COMPLETED,
        NotificationType.ADMIN_PAYMENT_ISSUE,
        -> "관리자"
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
