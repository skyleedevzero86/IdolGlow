package com.sleekydz86.idolglow.notification.graphql

import com.sleekydz86.idolglow.global.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.application.NotificationQueryService
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
    fun notifications(): List<NotificationGraphQlResponse> =
        notificationQueryService.findNotifications(authenticatedUserIdResolver.resolveRequired())
            .map(NotificationGraphQlResponse::from)

    @MutationMapping
    fun markNotificationRead(@Argument notificationId: String): NotificationGraphQlResponse =
        NotificationGraphQlResponse.from(
            notificationCommandService.markRead(
                notificationId = notificationId.toGraphQlIdLong("notificationId"),
                userId = authenticatedUserIdResolver.resolveRequired()
            )
        )
}
