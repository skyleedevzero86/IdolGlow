package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.dto.ContentCommentTarget
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentViewer
import com.sleekydz86.idolglow.comment.application.port.out.ContentCommentAuthorPort
import com.sleekydz86.idolglow.comment.application.port.out.ContentCommentPort
import com.sleekydz86.idolglow.comment.domain.ContentComment
import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

@DisplayName("댓글 트리 조립")
class ContentCommentServiceTest {
    private val port = mockk<ContentCommentPort>()
    private val authorPort = mockk<ContentCommentAuthorPort>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val service = ContentCommentService(port, authorPort, publisher, emptyList())
    private val base = LocalDateTime.of(2026, 11, 14, 10, 0)

    @DisplayName("한 페이지의 답글은 한 번의 조회로 불러와 오래된 순서로 트리를 만든다")
    @Test
    fun assemblesTreeWithSingleReplyQuery() {
        // given
        val rootA = comment(id = 1, minute = 0)
        val rootB = comment(id = 2, minute = 1)
        val laterReply = comment(id = 3, minute = 5, parentId = 1, rootId = 1, depth = 2)
        val earlierReply = comment(id = 4, minute = 3, parentId = 1, rootId = 1, depth = 2)
        val nested = comment(id = 5, minute = 6, parentId = 4, rootId = 1, depth = 3)
        val replyOnB = comment(id = 6, minute = 2, parentId = 2, rootId = 2, depth = 2)
        every { port.findThreadRoots(any(), any(), false) } returns OffsetSlice(listOf(rootA, rootB), 2)
        every { port.findRepliesByRootIds(listOf(1L, 2L)) } returns listOf(laterReply, earlierReply, nested, replyOnB)
        every { authorPort.findNicknames(any()) } returns mapOf(7L to "작성자")

        // when
        val result =
            service.findComments(
                ContentCommentTarget(ContentTargetType.ARTICLE, "a1"),
                ContentCommentViewer(userId = 7L),
                OffsetPageRequest.of(0, 20),
                latestFirst = false,
            )

        // then
        assertEquals(listOf(1L, 2L), result.items.map { it.id })
        assertEquals(listOf(4L, 3L), result.items[0].replies.map { it.id })
        assertEquals(listOf(5L), result.items[0].replies[0].replies.map { it.id })
        assertEquals(listOf(6L), result.items[1].replies.map { it.id })
        assertEquals(true, result.items[0].mine)
        verify(exactly = 1) { port.findRepliesByRootIds(any()) }
        verify(exactly = 1) { authorPort.findNicknames(any()) }
    }

    @DisplayName("삭제된 답글이 자식이 없으면 트리에서 빠진다")
    @Test
    fun dropsDeletedLeafReplies() {
        // given
        val root = comment(id = 1, minute = 0)
        val deletedLeaf = comment(id = 2, minute = 1, parentId = 1, rootId = 1, depth = 2).apply { delete(base) }
        every { port.findThreadRoots(any(), any(), false) } returns OffsetSlice(listOf(root), 1)
        every { port.findRepliesByRootIds(listOf(1L)) } returns listOf(deletedLeaf)
        every { authorPort.findNicknames(any()) } returns emptyMap()

        // when
        val result =
            service.findComments(
                ContentCommentTarget(ContentTargetType.ARTICLE, "a1"),
                ContentCommentViewer.ANONYMOUS,
                OffsetPageRequest.of(0, 20),
                latestFirst = false,
            )

        // then
        assertEquals(emptyList<Long>(), result.items.single().replies.map { it.id })
    }

    private fun comment(
        id: Long,
        minute: Long,
        parentId: Long? = null,
        rootId: Long? = null,
        depth: Int = 1,
    ): ContentComment =
        ContentComment(
            id = id,
            userId = 7L,
            targetType = ContentTargetType.ARTICLE,
            targetKey = "a1",
            targetTitle = "기사",
            contentMarkdown = "댓글 $id",
            parentId = parentId,
            rootId = rootId,
            depth = depth,
        ).also { setCreatedAt(it, base.plusMinutes(minute)) }

    private fun setCreatedAt(
        comment: ContentComment,
        createdAt: LocalDateTime,
    ) {
        val field = BaseEntity::class.java.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(comment, createdAt)
    }
}
