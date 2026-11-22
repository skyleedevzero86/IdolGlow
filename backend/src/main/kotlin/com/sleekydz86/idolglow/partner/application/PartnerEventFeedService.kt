package com.sleekydz86.idolglow.partner.application

import com.sleekydz86.idolglow.eventinfo.application.EventExternalIdService
import com.sleekydz86.idolglow.eventinfo.application.EventSessionQueryService
import com.sleekydz86.idolglow.eventinfo.application.ExternalIdView
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.ExternalIdScheme
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.partner.PartnerExceptionType
import com.sleekydz86.idolglow.partner.application.dto.PartnerEventItem
import com.sleekydz86.idolglow.partner.application.dto.PartnerEventPage
import com.sleekydz86.idolglow.partner.application.dto.PartnerExternalId
import com.sleekydz86.idolglow.partner.application.dto.PartnerFeedQuery
import com.sleekydz86.idolglow.partner.application.dto.PartnerSession
import com.sleekydz86.idolglow.partner.domain.PartnerClient
import com.sleekydz86.idolglow.partner.domain.PartnerFeedCursor
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class PartnerEventFeedService(
    private val partnerAccessService: PartnerAccessService,
    private val eventMasterPort: EventMasterPort,
    private val eventTranslationPort: EventTranslationPort,
    private val eventExternalIdService: EventExternalIdService,
    private val eventSessionQueryService: EventSessionQueryService,
) {
    internal var clock: Clock = Clock.systemDefaultZone()

    fun feed(
        rawKey: String?,
        query: PartnerFeedQuery,
    ): PartnerEventPage {
        val client = partnerAccessService.authorize(rawKey, FEED_PATH)
        return served(client, FEED_PATH, { it.items.size }) { page(client, query) }
    }

    fun event(
        rawKey: String?,
        globalId: String,
        lang: String?,
    ): PartnerEventItem {
        val client = partnerAccessService.authorize(rawKey, EVENT_PATH)
        return served(client, EVENT_PATH, { 1 }) { single(client, globalId, lang) }
    }

    fun sessions(
        rawKey: String?,
        globalId: String,
        from: LocalDate?,
        to: LocalDate?,
    ): List<PartnerSession> {
        val client = partnerAccessService.authorize(rawKey, SESSIONS_PATH)
        return served(client, SESSIONS_PATH, { it.size }) {
            val master =
                eventMasterPort.findByGlobalId(globalId.trim())
                    ?: throw CustomException(PartnerExceptionType.EVENT_NOT_FOUND)
            eventSessionQueryService
                .sessions(master.globalId, from, to)
                .map { PartnerSession(it.startsAt, it.status.name) }
        }
    }

    fun eventByExternalId(
        rawKey: String?,
        scheme: String,
        externalId: String,
        lang: String?,
    ): PartnerEventItem {
        val client = partnerAccessService.authorize(rawKey, LOOKUP_PATH)
        return served(client, LOOKUP_PATH, { 1 }) {
            val globalId =
                eventExternalIdService.resolve(ExternalIdScheme.parse(scheme), externalId)
                    ?: throw CustomException(PartnerExceptionType.EVENT_NOT_FOUND)
            single(client, globalId, lang)
        }
    }

    private fun single(
        client: PartnerClient,
        globalId: String,
        lang: String?,
    ): PartnerEventItem {
        val locale = servedLocale(client, lang)
        val master =
            eventMasterPort.findByGlobalId(globalId.trim())
                ?: throw CustomException(PartnerExceptionType.EVENT_NOT_FOUND)
        val externalIds = eventExternalIdService.idsOf(listOf(master.globalId))[master.globalId].orEmpty()
        val translations = eventTranslationPort.findByGlobalId(master.globalId)
        return item(master, client.fallbackOrder(locale), translations, externalIds)
    }

    private fun page(
        client: PartnerClient,
        query: PartnerFeedQuery,
    ): PartnerEventPage {
        val order = client.fallbackOrder(servedLocale(client, query.lang))
        val size = query.size.coerceIn(1, MAX_SIZE)
        val start =
            query.cursor?.takeIf { it.isNotBlank() }?.let(PartnerFeedCursor::decode)
                ?: PartnerFeedCursor.since(query.updatedSince)
        val until = LocalDateTime.now(clock).minus(SAFETY_LAG)
        val rows = eventMasterPort.findChangedAfter(start.updatedAt, start.globalId, until, size + 1)
        val pageRows = rows.take(size)
        val globalIds = pageRows.map { it.globalId }
        val translations = eventTranslationPort.findByGlobalIdIn(globalIds).groupBy { it.globalId }
        val externalIds = eventExternalIdService.idsOf(globalIds)
        val last = pageRows.lastOrNull()?.let { PartnerFeedCursor(checkNotNull(it.updatedAt), it.globalId) } ?: start
        return PartnerEventPage(
            items =
                pageRows.map {
                    item(it, order, translations[it.globalId].orEmpty(), externalIds[it.globalId].orEmpty())
                },
            nextCursor = last.takeIf { it.globalId.isNotEmpty() }?.encode() ?: query.cursor,
            hasMore = rows.size > size,
        )
    }

    private fun servedLocale(
        client: PartnerClient,
        lang: String?,
    ): String {
        val locale = lang?.takeIf { it.isNotBlank() }?.let(EventLocale::normalize) ?: client.defaultLocale()
        if (!client.serves(locale)) throw CustomException(PartnerExceptionType.LOCALE_NOT_CONTRACTED)
        return locale
    }

    private fun item(
        master: EventMaster,
        order: List<String>,
        translations: List<EventTranslation>,
        externalIds: List<ExternalIdView>,
    ): PartnerEventItem {
        val visible = translations.filter { it.origin.visible }
        val picked = order.firstNotNullOfOrNull { locale -> visible.find { it.locale == locale } }
        return PartnerEventItem(
            globalId = master.globalId,
            status = master.status.name,
            mergedInto = master.mergedInto,
            locale = picked?.locale ?: EventLocale.KOREAN,
            title = picked?.title ?: master.title,
            synopsis = picked?.synopsis,
            startDate = master.startDate,
            endDate = master.endDate,
            venue = master.venue,
            updatedAt = master.updatedAt?.atZone(ZoneId.systemDefault())?.toOffsetDateTime(),
            externalIds = externalIds.map { PartnerExternalId(it.scheme, it.id) },
        )
    }

    private fun <T> served(
        client: PartnerClient,
        path: String,
        count: (T) -> Int,
        block: () -> T,
    ): T {
        val result =
            try {
                block()
            } catch (e: CustomException) {
                partnerAccessService.record(client, path, e.getExceptionType().httpStatusCode, 0)
                throw e
            } catch (e: IllegalArgumentException) {
                partnerAccessService.record(client, path, BAD_REQUEST, 0)
                throw e
            }
        partnerAccessService.record(client, path, OK, count(result))
        return result
    }

    companion object {
        const val FEED_PATH: String = "/partner/v1/events"
        const val EVENT_PATH: String = "/partner/v1/events/{globalId}"
        const val LOOKUP_PATH: String = "/partner/v1/events/by-external-id"
        const val SESSIONS_PATH: String = "/partner/v1/events/{globalId}/sessions"
        const val MAX_SIZE: Int = 200
        val SAFETY_LAG: Duration = Duration.ofSeconds(5)
        private const val OK: Int = 200
        private const val BAD_REQUEST: Int = 400
    }
}
