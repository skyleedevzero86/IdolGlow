package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityReportReasonResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityReportedPostResult
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityAdminUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityReactionPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class GlowCommunityAdminService(
    private val glowCommunityPostPort: GlowCommunityPostPort,
    private val glowCommunityReactionPort: GlowCommunityReactionPort,
    private val glowCommunityPostAssembler: GlowCommunityPostAssembler,
) : GlowCommunityAdminUseCase {
    @Transactional(readOnly = true)
    override fun findReportedPosts(request: OffsetPageRequest): OffsetPageResult<GlowCommunityReportedPostResult> {
        val normalized = OffsetPageRequest.of(request.page, request.size)
        val slice = glowCommunityPostPort.findReported(normalized)
        return OffsetPageResult.of(toResults(slice.items), normalized, slice.totalElements)
    }

    override fun blind(
        postId: Long,
        reason: String?,
    ): GlowCommunityReportedPostResult {
        val post = loadPost(postId)
        post.blind(LocalDateTime.now(), reason.orEmpty())
        return toResults(listOf(glowCommunityPostPort.save(post))).first()
    }

    override fun unblind(postId: Long): GlowCommunityReportedPostResult {
        val post = loadPost(postId)
        post.unblind()
        return toResults(listOf(glowCommunityPostPort.save(post))).first()
    }

    private fun loadPost(postId: Long): GlowCommunityPost =
        glowCommunityPostPort.findActiveByIdForUpdate(postId)
            ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)

    private fun toResults(posts: List<GlowCommunityPost>): List<GlowCommunityReportedPostResult> {
        if (posts.isEmpty()) return emptyList()
        val nicknames = glowCommunityPostAssembler.nicknamesOf(posts.map { it.userId })
        val reportsByPostId =
            glowCommunityReactionPort
                .findReportsByPostIds(posts.map { it.id })
                .groupBy { it.postId }
        return posts.map { post ->
            GlowCommunityReportedPostResult(
                postId = post.id,
                tag = post.tag.code,
                title = post.title,
                authorId = post.userId,
                authorName = nicknames[post.userId] ?: GlowCommunityPostAssembler.UNKNOWN_AUTHOR,
                reportCount = post.reportCount,
                lastReportedAt = post.lastReportedAt,
                blinded = post.blinded,
                blindedAt = post.blindedAt,
                blindReason = post.blindReason,
                createdAt = post.createdAt,
                reports =
                    reportsByPostId[post.id]
                        .orEmpty()
                        .sortedByDescending { it.createdAt }
                        .map { GlowCommunityReportReasonResult(reason = it.reason, reportedAt = it.createdAt) },
            )
        }
    }
}
