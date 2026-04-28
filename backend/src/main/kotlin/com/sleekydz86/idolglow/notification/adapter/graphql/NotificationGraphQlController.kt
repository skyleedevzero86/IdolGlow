package com.sleekydz86.idolglow.notification.graphql

import com.sleekydz86.idolglow.global.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.application.NotificationQueryService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class NotificationGraphQlController(
    private val notificationQueryService: NotificationQueryService,
    private val notificationCommandService: NotificationCommandService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun notifications(@Argument type: String?): List<NotificationGraphQlResponse> {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val notificationType = type?.let { NotificationType.valueOf(it) }
        return notificationQueryService.findNotifications(userId, notificationType)
            .map(NotificationGraphQlResponse::from)
    }

    @QueryMapping
    fun notificationUnreadCount(): Long =
        notificationQueryService.countUnread(authenticatedUserIdResolver.resolveRequired()).count

    @MutationMapping
    fun markNotificationRead(@Argument notificationId: String): NotificationGraphQlResponse =
        NotificationGraphQlResponse.from(
            notificationCommandService.markRead(
                notificationId = notificationId.toGraphQlIdLong("notificationId"),
                userId = authenticatedUserIdResolver.resolveRequired()
            )
        )

    @MutationMapping
    fun markAllNotificationsRead(): Boolean {
        notificationCommandService.markAllRead(authenticatedUserIdResolver.resolveRequired())
        return true
    }
}
