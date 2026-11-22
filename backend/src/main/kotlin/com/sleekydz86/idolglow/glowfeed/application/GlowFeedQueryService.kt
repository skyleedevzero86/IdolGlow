package com.sleekydz86.idolglow.glowfeed.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedEntry
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedItemResult
import com.sleekydz86.idolglow.glowfeed.application.port.`in`.GlowFeedQueryUseCase
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedContentPort
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedIndexPort
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedViewerCountPort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedCategory
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedHighlightSort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import com.sleekydz86.idolglow.like.application.dto.LikeSummary
import com.sleekydz86.idolglow.like.application.port.`in`.ContentLikeQueryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class GlowFeedQueryService(
    private val glowFeedIndexPort: GlowFeedIndexPort,
    private val glowFeedViewerCountPort: GlowFeedViewerCountPort,
    private val contentLikeQueryUseCase: ContentLikeQueryUseCase,
    contentPorts: List<GlowFeedContentPort>,
) : GlowFeedQueryUseCase {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    private val contentPorts: Map<GlowFeedKind, GlowFeedContentPort> = contentPorts.associateBy { it.kind }

    override fun findFeed(
        category: GlowFeedCategory?,
        request: OffsetPageRequest,
        viewerId: Long?,
    ): OffsetPageResult<GlowFeedItemResult> {
        val slice = glowFeedIndexPort.findPage(category, request, today())
        return OffsetPageResult.of(
            items = assemble(slice.items, viewerId),
            request = request,
            totalElements = slice.totalElements,
        )
    }

    override fun findHighlights(
        sort: GlowFeedHighlightSort,
        viewerId: Long?,
    ): List<GlowFeedItemResult> {
        val today = today()
        val entries =
            GlowFeedCategory.entries.mapNotNull { category ->
                when (sort) {
                    GlowFeedHighlightSort.LATEST -> glowFeedIndexPort.findLatest(category, today)
                    GlowFeedHighlightSort.POPULAR -> glowFeedIndexPort.findMostPopular(category, today)
                }
            }
        return assemble(entries, viewerId)
    }

    private fun assemble(
        entries: List<GlowFeedEntry>,
        viewerId: Long?,
    ): List<GlowFeedItemResult> {
        if (entries.isEmpty()) return emptyList()
        val contents =
            entries
                .groupBy { it.kind }
                .mapValues { (kind, grouped) -> contentPorts[kind]?.load(grouped.map { it.refId }).orEmpty() }
        val likes = contentLikeQueryUseCase.summarize(entries.map { it.likeTarget }, viewerId)
        val viewers = glowFeedViewerCountPort.countViewers(entries.map { it.viewTarget }.distinct())
        return entries.mapNotNull { entry ->
            val content = contents[entry.kind]?.get(entry.refId) ?: return@mapNotNull null
            GlowFeedItemResult.of(
                entry = entry,
                content = content,
                likes = likes[entry.likeTarget] ?: LikeSummary.EMPTY,
                viewerCount = viewers[entry.viewTarget] ?: 0L,
            )
        }
    }

    private fun today(): LocalDate = LocalDate.now(clock)
}
