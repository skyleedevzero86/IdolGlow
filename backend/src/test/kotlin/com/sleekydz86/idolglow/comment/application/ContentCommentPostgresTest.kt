package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.dto.ContentCommentResult
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentTarget
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentViewer
import com.sleekydz86.idolglow.comment.application.dto.CreateContentCommentCommand
import com.sleekydz86.idolglow.comment.application.port.`in`.ContentCommentUseCase
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.comment.ContentCommentExceptionType
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.glowcommunity.application.dto.CreateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostCommandUseCase
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@DisplayName("콘텐츠 공용 댓글")
class ContentCommentPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var commentUseCase: ContentCommentUseCase

    @Autowired
    private lateinit var postCommandUseCase: GlowCommunityPostCommandUseCase

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("대상마다 따로 댓글을 쓰고 본다")
    @Test
    fun createAndListPerTarget() {
        // given
        val writer = member()
        val article = key()
        val product = key()
        create(writer, ContentTargetType.ARTICLE, article, "기사 댓글 ![사진](/api/mbrd/editor/images/abc)")
        create(writer, ContentTargetType.PRODUCT, product, "상품 댓글")

        // when
        val articleComments = list(ContentTargetType.ARTICLE, article, ContentCommentViewer(writer.id))
        val productComments = list(ContentTargetType.PRODUCT, product, ContentCommentViewer.ANONYMOUS)

        // then
        assertEquals(listOf("기사 댓글 ![사진](/api/mbrd/editor/images/abc)"), articleComments.map { it.contentMarkdown })
        assertTrue(articleComments.single().mine)
        assertEquals(writer.nickname.value, articleComments.single().authorName)
        assertEquals(listOf("상품 댓글"), productComments.map { it.contentMarkdown })
        assertFalse(productComments.single().mine)
    }

    @DisplayName("관리자 경로나 외부 주소는 대상 경로로 받지 않는다")
    @Test
    fun rejectsUnsafeTargetPath() {
        // given
        val writer = member()

        // when
        val admin = invalid { create(writer, ContentTargetType.ARTICLE, key(), "본문", path = "/admin/users") }
        val external = invalid { create(writer, ContentTargetType.ARTICLE, key(), "본문", path = "https://evil.example") }

        // then
        assertTrue(admin.message!!.contains("관리자"))
        assertTrue(external.message!!.contains("/"))
    }

    @DisplayName("본인 댓글만 고치고 지울 수 있고 관리자는 누구의 댓글이든 지울 수 있다")
    @Test
    fun ownOnlyEditAndDelete() {
        // given
        val writer = member()
        val other = member()
        val target = key()
        val comment = create(writer, ContentTargetType.ARTICLE, target, "원래 내용")
        val second = create(writer, ContentTargetType.ARTICLE, target, "관리자가 지울 댓글")

        // when
        val editError = denied { commentUseCase.update(comment.id, other.id, "남이 고침") }
        val deleteError = denied { commentUseCase.delete(comment.id, ContentCommentViewer(other.id)) }
        val edited = commentUseCase.update(comment.id, writer.id, "고친 내용")
        commentUseCase.delete(second.id, ContentCommentViewer(userId = other.id, admin = true))

        // then
        assertEquals(ContentCommentExceptionType.NOT_COMMENT_OWNER, editError.getExceptionType())
        assertEquals(ContentCommentExceptionType.NOT_COMMENT_OWNER, deleteError.getExceptionType())
        assertEquals("고친 내용", edited.contentMarkdown)
        assertEquals(listOf(comment.id), list(ContentTargetType.ARTICLE, target).map { it.id })
    }

    @DisplayName("답글 없는 삭제 댓글은 숨기고 답글 있는 삭제 댓글은 자리만 남긴다")
    @Test
    fun deletedLeafHiddenAndDeletedParentPlaceholder() {
        // given
        val writer = member()
        val replier = member()
        val target = key()
        val leaf = create(writer, ContentTargetType.GLOW, target, "지울 댓글")
        val parent = create(writer, ContentTargetType.GLOW, target, "답글 달린 댓글")
        create(replier, ContentTargetType.GLOW, target, "답글", parentId = parent.id)

        // when
        commentUseCase.delete(leaf.id, ContentCommentViewer(writer.id))
        commentUseCase.delete(parent.id, ContentCommentViewer(writer.id))
        val tree = list(ContentTargetType.GLOW, target)

        // then
        assertEquals(listOf(parent.id), tree.map { it.id })
        val placeholder = tree.single()
        assertTrue(placeholder.deleted)
        assertEquals(ContentCommentTreeAssembler.DELETED_PLACEHOLDER, placeholder.contentMarkdown)
        assertNull(placeholder.authorName)
        assertNull(placeholder.authorId)
        assertEquals(listOf("답글"), placeholder.replies.map { it.contentMarkdown })
    }

    @DisplayName("답글은 5단계까지 달 수 있고 6단계는 거절한다")
    @Test
    fun replyDepthLimitedToFive() {
        // given
        val writer = member()
        val target = key()
        var parent = create(writer, ContentTargetType.SUBWAY, target, "1단계")
        (2..5).forEach { depth ->
            parent = create(writer, ContentTargetType.SUBWAY, target, "${depth}단계", parentId = parent.id)
        }

        // when
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                create(writer, ContentTargetType.SUBWAY, target, "6단계", parentId = parent.id)
            }
        val tree = list(ContentTargetType.SUBWAY, target)

        // then
        assertEquals(5, parent.depth)
        assertTrue(error.message!!.contains("5단계"))
        var node: ContentCommentResult = tree.single()
        val depths = mutableListOf(node.depth)
        while (node.replies.isNotEmpty()) {
            node = node.replies.single()
            depths += node.depth
        }
        assertEquals(listOf(1, 2, 3, 4, 5), depths)
    }

    @DisplayName("다른 콘텐츠의 댓글이나 삭제된 댓글에는 답글을 달 수 없다")
    @Test
    fun replyRejectsForeignOrDeletedParent() {
        // given
        val writer = member()
        val target = key()
        val foreign = create(writer, ContentTargetType.ARTICLE, key(), "다른 기사")
        val deleted = create(writer, ContentTargetType.ARTICLE, target, "지운 댓글")
        commentUseCase.delete(deleted.id, ContentCommentViewer(writer.id))

        // when
        val foreignError =
            assertThrows(IllegalArgumentException::class.java) {
                create(writer, ContentTargetType.ARTICLE, target, "답글", parentId = foreign.id)
            }
        val deletedError =
            assertThrows(IllegalArgumentException::class.java) {
                create(writer, ContentTargetType.ARTICLE, target, "답글", parentId = deleted.id)
            }

        // then
        assertTrue(foreignError.message!!.contains("다른 콘텐츠"))
        assertTrue(deletedError.message!!.contains("삭제된"))
    }

    @DisplayName("나의 리뷰는 모든 대상의 내 댓글과 답글을 최신순으로 모으고 지운 댓글은 뺀다")
    @Test
    fun myReviewsAggregateAcrossTargets() {
        // given
        val me = member()
        val other = member()
        val parent = create(other, ContentTargetType.PRODUCT, key(), "상품 문의")
        val first = create(me, ContentTargetType.ARTICLE, key(), "기사 **댓글**")
        val removed = create(me, ContentTargetType.EXCHANGE_RATE, key(), "지울 댓글")
        val reply = create(me, ContentTargetType.PRODUCT, parent.targetKey, "상품 답글", parentId = parent.id)
        commentUseCase.delete(removed.id, ContentCommentViewer(me.id))

        // when
        val reviews = commentUseCase.findMyReviews(me.id, OffsetPageRequest.of(0, 20))

        // then
        assertEquals(listOf(reply.id, first.id), reviews.items.map { it.commentId })
        assertEquals(listOf("PRODUCT", "ARTICLE"), reviews.items.map { it.targetType })
        assertEquals(2, reviews.items.first().depth)
        assertEquals("상품 문의", reviews.items.first().parentExcerpt)
        assertEquals("기사 댓글", reviews.items.last().excerpt)
        assertEquals(2L, reviews.totalElements)
    }

    @DisplayName("게시글에 댓글이 달리면 작성자에게 알림이 가고 링크는 대상 경로와 댓글 위치다")
    @Test
    fun postAuthorNotifiedOnComment() {
        // given
        val author = member()
        val commenter = member()
        val post = post(author)

        // when
        val comment = onPost(commenter, post, "좋은 글이에요")

        // then
        val row = notifications(author.id).single()
        assertEquals("COMMENT_ON_MY_POST", row["type"])
        assertEquals("/glow?post=$post#comment-${comment.id}", row["link"])
        assertTrue((row["message"] as String).startsWith("${commenter.nickname.value}님이 내 게시글"))
        assertTrue(notifications(commenter.id).isEmpty())
    }

    @DisplayName("답글이 달리면 부모 댓글 작성자와 게시글 작성자에게 한 번씩 알림이 간다")
    @Test
    fun parentAndPostAuthorNotifiedOnReply() {
        // given
        val author = member()
        val commenter = member()
        val replier = member()
        val post = post(author)
        val parent = onPost(commenter, post, "첫 댓글")
        val beforeAuthor = notifications(author.id).size

        // when
        onPost(replier, post, "답글", parentId = parent.id)

        // then
        assertEquals(listOf("REPLY_TO_MY_COMMENT"), notifications(commenter.id).map { it["type"] })
        assertEquals(beforeAuthor + 1, notifications(author.id).size)
        assertEquals("COMMENT_ON_MY_POST", notifications(author.id).last()["type"])
    }

    @DisplayName("부모 댓글 작성자가 게시글 작성자면 알림은 하나만 가고 자기 글에 단 댓글은 알리지 않는다")
    @Test
    fun duplicateSuppressedAndNoSelfNotification() {
        // given
        val author = member()
        val replier = member()
        val post = post(author)
        val own = onPost(author, post, "내 글 내 댓글")

        // when
        onPost(replier, post, "답글", parentId = own.id)

        // then
        assertEquals(listOf("REPLY_TO_MY_COMMENT"), notifications(author.id).map { it["type"] })
    }

    @DisplayName("관리자가 댓글을 달면 알림이 없고 관리자는 회원 알림을 받지 않는다")
    @Test
    fun adminsExcludedFromCommentNotifications() {
        // given
        val admin = member(UserRole.ADMIN)
        val author = member()
        val commenter = member()
        val adminPost = post(admin)
        val post = post(author)

        // when
        onPost(admin, post, "관리자 댓글")
        val adminComment = create(admin, ContentTargetType.ARTICLE, key(), "관리자 기사 댓글", path = "/articles/1")
        val articleKey = adminComment.targetKey
        create(commenter, ContentTargetType.ARTICLE, articleKey, "답글", adminComment.id, "/articles/1")
        onPost(commenter, adminPost, "관리자 글 댓글")

        // then
        assertTrue(notifications(author.id).isEmpty())
        val memberTypes = setOf("COMMENT_ON_MY_POST", "REPLY_TO_MY_COMMENT")
        assertTrue(notifications(admin.id).none { it["type"] in memberTypes })
    }

    @DisplayName("다른 콘텐츠의 댓글은 답글만 알린다")
    @Test
    fun otherTargetsNotifyOnlyReplies() {
        // given
        val writer = member()
        val replier = member()
        val target = key()
        val parent = create(writer, ContentTargetType.PRODUCT, target, "상품 댓글", path = "/products/7")

        // when
        val reply = create(replier, ContentTargetType.PRODUCT, target, "답글", parentId = parent.id, path = "/products/7")

        // then
        val rows = notifications(writer.id)
        assertEquals(listOf("REPLY_TO_MY_COMMENT"), rows.map { it["type"] })
        assertEquals("/products/7#comment-${reply.id}", rows.single()["link"])
        assertEquals("${replier.nickname.value}님이 내 댓글에 답글을 남겼어요.", rows.single()["message"])
    }

    private fun create(
        user: User,
        type: ContentTargetType,
        targetKey: String,
        content: String,
        parentId: Long? = null,
        path: String? = "/contents/$targetKey",
    ): ContentCommentResult =
        commentUseCase.create(
            CreateContentCommentCommand(
                userId = user.id,
                targetType = type,
                targetKey = targetKey,
                targetTitle = "제목 $targetKey",
                targetPath = path,
                contentMarkdown = content,
                parentId = parentId,
            ),
        )

    private fun list(
        type: ContentTargetType,
        targetKey: String,
        viewer: ContentCommentViewer = ContentCommentViewer.ANONYMOUS,
    ): List<ContentCommentResult> =
        commentUseCase
            .findComments(ContentCommentTarget(type, targetKey), viewer, OffsetPageRequest.of(0, 20), false)
            .items

    private fun post(author: User): Long =
        postCommandUseCase
            .create(CreateGlowCommunityPostCommand(author.id, GlowCommunityTag.TALK, "알림 게시글 ${key()}", "본문"))
            .id

    private fun notifications(userId: Long): List<Map<String, Any?>> =
        jdbcTemplate.queryForList("select type, link, message from notifications where user_id = ? order by id", userId)

    private fun member(role: UserRole = UserRole.USER): User {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val user = User(email = "cc$suffix@test.com", nickname = Nickname.of("c$suffix"), role = role)
        return userJpaRepository.save(user)
    }

    private fun onPost(
        user: User,
        postId: Long,
        content: String,
        parentId: Long? = null,
    ): ContentCommentResult =
        create(user, ContentTargetType.GLOW_COMMUNITY_POST, postId.toString(), content, parentId, "/glow?post=$postId")

    private fun invalid(action: () -> Unit): IllegalArgumentException =
        assertThrows(IllegalArgumentException::class.java) { action() }

    private fun denied(action: () -> Unit): CustomException = assertThrows(CustomException::class.java) { action() }

    private fun key(): String = UUID.randomUUID().toString().take(12)
}
