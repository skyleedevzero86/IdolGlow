package com.sleekydz86.idolglow.eventinfo.adapter.graphql

import com.sleekydz86.idolglow.eventinfo.application.EventLocalizedTextService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class EventContentGraphQlController(
    private val eventLocalizedTextService: EventLocalizedTextService,
) {
    @QueryMapping
    fun eventContent(
        @Argument globalId: String,
    ): EventContentGraphQl? =
        eventLocalizedTextService.title(globalId, null)?.let { EventContentGraphQl(globalId.trim()) }

    @SchemaMapping(typeName = "EventContent", field = "title")
    fun title(
        content: EventContentGraphQl,
        @Argument locale: String?,
    ): EventLocalizedTextGraphQl {
        val resolved = eventLocalizedTextService.title(content.globalId, locale)
        requireNotNull(resolved) { "공연을 찾을 수 없습니다. globalId=${content.globalId}" }
        return EventLocalizedTextGraphQl(
            title = resolved.title,
            synopsis = resolved.synopsis,
            locale = resolved.locale,
        )
    }
}

data class EventContentGraphQl(
    val globalId: String,
)

data class EventLocalizedTextGraphQl(
    val title: String,
    val synopsis: String?,
    val locale: String,
)
