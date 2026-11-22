package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.comment.application.dto.CreateContentCommentCommand
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentViewer
import com.sleekydz86.idolglow.comment.application.port.`in`.ContentCommentUseCase
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.glowcommunity.application.dto.CreateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostSearchQuery
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityAdminUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostCommandUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostQueryUseCase
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunitySort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@DisplayName("GLOW 지역사회 게시판")
class GlowCommunityPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var commandUseCase: GlowCommunityPostCommandUseCase

    @Autowired
    private lateinit var queryUseCase: GlowCommunityPostQueryUseCase

    @Autowired
    private lateinit var adminUseCase: GlowCommunityAdminUseCase

    @Autowired
    private lateinit var commentUseCase: ContentCommentUseCase

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("글을 쓰면 목록에 나오고 말머리와 검색어로 거를 수 있다")
    @Test
    fun createListFilterAndSearch() {
        // given
        val author = member()
        val token = token()
        val talk = write(author, GlowCommunityTag.TALK, "$token 명동 맛집", "## 본문\n**마크다운**")
        val question = write(author, GlowCommunityTag.QUESTION, "질문 $token", "배달 앱 있나요")
        write(author, GlowCommunityTag.TRAVEL, "다른 글", "본문에 $token 이 있어요")

        // when
        val all = search(keyword = token)
        val questionsOnly = search(keyword = token, tag = GlowCommunityTag.QUESTION)
        val byTitle = search(keyword = "$token 명동")

        // then
        assertEquals(3L, all.totalElements)
        assertEquals(listOf(question.id), questionsOnly.items.map { it.id })
        assertEquals(listOf(talk.id), byTitle.items.map { it.id })
        assertEquals("talk", talk.tag)
        assertEquals("## 본문\n**마크다운**", talk.content)
        assertTrue(talk.mine)
        assertEquals(author.nickname.value, talk.authorName)
    }

    @DisplayName("좋아요는 회원마다 한 번이고 다시 누르면 취소된다")
    @Test
    fun likeToggleIsIdempotentPerMember() {
        // given
        val author = member()
        val first = member()
        val second = member()
        val post = write(author, GlowCommunityTag.TALK, "좋아요 ${token()}", "본문")

        // when
        val liked = commandUseCase.toggleLike(post.id, first.id)
        val unliked = commandUseCase.toggleLike(post.id, first.id)
        commandUseCase.toggleLike(post.id, first.id)
        val secondLike = commandUseCase.toggleLike(post.id, second.id)

        // then
        assertTrue(liked.liked)
        assertEquals(1L, liked.likeCount)
        assertFalse(unliked.liked)
        assertEquals(0L, unliked.likeCount)
        assertEquals(2L, secondLike.likeCount)
        val view = queryUseCase.findPost(post.id, GlowCommunityViewer.member(first.id))
        assertTrue(view.likedByMe)
        assertTrue(view.liked)
        assertFalse(queryUseCase.findPost(post.id, GlowCommunityViewer.member(author.id)).likedByMe)
    }

    @DisplayName("댓글과 답글을 달고 지우면 게시글 댓글 수가 함께 바뀐다")
    @Test
    fun commentCountFollowsComments() {
        // given
        val author = member()
        val commenter = member()
        val post = write(author, GlowCommunityTag.QUESTION, "댓글 ${token()}", "본문")
        val comment = comment(commenter, post.id, "첫 댓글")
        comment(author, post.id, "답글", parentId = comment.id)

        // when
        val afterTwo = queryUseCase.findPost(post.id, GlowCommunityViewer.ANONYMOUS).commentCount
        commentUseCase.delete(comment.id, ContentCommentViewer(userId = commenter.id))
        val afterDelete = queryUseCase.findPost(post.id, GlowCommunityViewer.ANONYMOUS).commentCount

        // then
        assertEquals(2L, afterTwo)
        assertEquals(1L, afterDelete)
    }

    @DisplayName("하이라이트는 좋아요와 댓글 중 큰 값, 그다음 작은 값, 그다음 최신순이고 블라인드 글은 빠진다")
    @Test
    fun highlightsOrderByGreaterEngagement() {
        // given
        val author = member()
        val likesOnly = write(author, GlowCommunityTag.TALK, "좋아요만 ${token()}", "본문")
        val commentsOnly = write(author, GlowCommunityTag.TALK, "댓글만 ${token()}", "본문")
        val both = write(author, GlowCommunityTag.TALK, "둘 다 ${token()}", "본문")
        val newerLikes = write(author, GlowCommunityTag.TALK, "새 좋아요 ${token()}", "본문")
        val blinded = write(author, GlowCommunityTag.TALK, "블라인드 ${token()}", "본문")
        setCounts(likesOnly.id, likes = 900_005, comments = 0)
        setCounts(commentsOnly.id, likes = 0, comments = 900_010)
        setCounts(both.id, likes = 900_005, comments = 900_003)
        setCounts(newerLikes.id, likes = 900_005, comments = 0)
        setCounts(blinded.id, likes = 999_999, comments = 999_999)
        adminUseCase.blind(blinded.id, "테스트")

        // when
        val highlights = queryUseCase.findHighlights(GlowCommunityViewer.ANONYMOUS)

        // then
        assertEquals(listOf(commentsOnly.id, both.id, newerLikes.id, likesOnly.id), highlights.take(4).map { it.id })
        assertFalse(highlights.any { it.id == blinded.id })
        assertTrue(highlights.size <= GlowCommunityPostQueryService.HIGHLIGHT_LIMIT)
    }

    @DisplayName("서로 다른 회원 3명이 신고하면 자동 블라인드되고 같은 회원의 중복 신고는 무시한다")
    @Test
    fun autoBlindAfterThreeDistinctReports() {
        // given
        val author = member()
        val reporters = List(3) { member() }
        val token = token()
        val post = write(author, GlowCommunityTag.TALK, "$token 신고", "본문")

        // when
        commandUseCase.report(post.id, reporters[0].id, "광고")
        commandUseCase.report(post.id, reporters[0].id, "광고")
        commandUseCase.report(post.id, reporters[1].id, null)
        val beforeThird = queryUseCase.findPost(post.id, GlowCommunityViewer.ANONYMOUS)
        commandUseCase.report(post.id, reporters[2].id, null)

        // then
        assertFalse(beforeThird.blinded)
        assertEquals(3L, reportCount(post.id))
        val error = rejected { queryUseCase.findPost(post.id, GlowCommunityViewer.ANONYMOUS) }
        assertEquals(GlowCommunityExceptionType.POST_NOT_FOUND, error.getExceptionType())
        assertTrue(queryUseCase.findPost(post.id, GlowCommunityViewer.member(author.id)).blinded)
    }

    @DisplayName("관리자에게는 첫 신고와 자동 블라인드 때만 알림이 간다")
    @Test
    fun adminsNotifiedOnFirstReportAndAutoBlind() {
        // given
        val admin = member(UserRole.ADMIN)
        val author = member()
        val reporters = List(3) { member() }
        val token = token()
        val post = write(author, GlowCommunityTag.TALK, "$token 관리자 알림", "본문")

        // when
        reporters.forEach { commandUseCase.report(post.id, it.id, null) }

        // then
        assertEquals(1, notificationCount(admin.id, "ADMIN_POST_REPORTED", token))
        assertEquals(1, notificationCount(admin.id, "ADMIN_POST_AUTO_BLINDED", token))
        assertEquals(0, notificationCount(author.id, "ADMIN_POST_REPORTED", token))
        assertEquals(
            "/admin/glow-community/reports",
            jdbcTemplate.queryForObject(
                "select link from notifications where user_id = ? and type = 'ADMIN_POST_REPORTED' and message like ?",
                String::class.java,
                admin.id,
                "%$token%",
            ),
        )
    }

    @DisplayName("블라인드 글은 다른 회원에게 숨기고 작성자와 관리자에게만 표시와 함께 보인다")
    @Test
    fun blindedPostVisibleOnlyToAuthorAndAdmin() {
        // given
        val author = member()
        val other = member()
        val token = token()
        val post = write(author, GlowCommunityTag.TRAVEL, "$token 블라인드", "본문")
        adminUseCase.blind(post.id, "운영 정책")

        // when
        val otherList = search(keyword = token, viewer = GlowCommunityViewer.member(other.id))
        val authorList = search(keyword = token, viewer = GlowCommunityViewer.member(author.id))
        val adminView = queryUseCase.findPost(post.id, GlowCommunityViewer(userId = other.id, admin = true))

        // then
        assertTrue(otherList.items.isEmpty())
        assertEquals(listOf(post.id), authorList.items.map { it.id })
        assertTrue(authorList.items.single().blinded)
        assertTrue(adminView.blinded)
        rejected { queryUseCase.findPost(post.id, GlowCommunityViewer.member(other.id)) }
        val commentError =
            assertThrows(CustomException::class.java) { comment(other, post.id, "블라인드 글 댓글") }
        assertEquals(GlowCommunityExceptionType.POST_NOT_FOUND, commentError.getExceptionType())
        val authorCommentError =
            assertThrows(CustomException::class.java) { comment(author, post.id, "내 블라인드 글 댓글") }
        assertEquals(GlowCommunityExceptionType.BLINDED_POST, authorCommentError.getExceptionType())
    }

    @DisplayName("블라인드를 해제하면 다시 모두에게 보인다")
    @Test
    fun unblindRestoresVisibility() {
        // given
        val author = member()
        val post = write(author, GlowCommunityTag.TALK, "해제 ${token()}", "본문")
        adminUseCase.blind(post.id, null)

        // when
        val result = adminUseCase.unblind(post.id)

        // then
        assertFalse(result.blinded)
        assertFalse(queryUseCase.findPost(post.id, GlowCommunityViewer.ANONYMOUS).blinded)
    }

    @DisplayName("내 글 목록은 내 글만 보여 주고 블라인드 글도 표시와 함께 포함한다")
    @Test
    fun myPostsOnlyOwn() {
        // given
        val me = member()
        val other = member()
        val mine = write(me, GlowCommunityTag.TALK, "내 글 ${token()}", "본문")
        val blindedMine = write(me, GlowCommunityTag.QUESTION, "내 블라인드 ${token()}", "본문")
        write(other, GlowCommunityTag.TALK, "남의 글 ${token()}", "본문")
        adminUseCase.blind(blindedMine.id, null)

        // when
        val page = queryUseCase.findMyPosts(me.id, OffsetPageRequest.of(0, 20))

        // then
        assertEquals(listOf(blindedMine.id, mine.id), page.items.map { it.id })
        assertTrue(page.items.first().blinded)
        assertTrue(page.items.all { it.mine })
    }

    @DisplayName("다른 회원의 글은 수정하거나 삭제할 수 없고 삭제한 글은 목록에서 빠진다")
    @Test
    fun onlyOwnerEditsAndDeletes() {
        // given
        val author = member()
        val other = member()
        val token = token()
        val post = write(author, GlowCommunityTag.TALK, "$token 수정 전", "본문")

        // when
        val error = assertThrows(CustomException::class.java) { commandUseCase.delete(post.id, other.id) }
        commandUseCase.delete(post.id, author.id)

        // then
        assertEquals(GlowCommunityExceptionType.NOT_POST_OWNER, error.getExceptionType())
        assertTrue(search(keyword = token).items.isEmpty())
    }

    private fun write(
        user: User,
        tag: GlowCommunityTag,
        title: String,
        content: String,
    ): GlowCommunityPostResult = commandUseCase.create(CreateGlowCommunityPostCommand(user.id, tag, title, content))

    private fun comment(
        user: User,
        postId: Long,
        content: String,
        parentId: Long? = null,
    ) = commentUseCase.create(
        CreateContentCommentCommand(
            userId = user.id,
            targetType = ContentTargetType.GLOW_COMMUNITY_POST,
            targetKey = postId.toString(),
            targetTitle = "게시글",
            targetPath = "/glow?post=$postId",
            contentMarkdown = content,
            parentId = parentId,
        ),
    )

    private fun search(
        keyword: String,
        tag: GlowCommunityTag? = null,
        viewer: GlowCommunityViewer = GlowCommunityViewer.ANONYMOUS,
    ) = queryUseCase.findPosts(
        GlowCommunityPostSearchQuery(tag, GlowCommunitySort.LATEST, keyword, 0, 20),
        viewer,
    )

    private fun setCounts(
        postId: Long,
        likes: Long,
        comments: Long,
    ) {
        val sql = "update glow_community_posts set like_count = ?, comment_count = ? where id = ?"
        jdbcTemplate.update(sql, likes, comments, postId)
    }

    private fun reportCount(postId: Long): Long =
        jdbcTemplate.queryForObject(
            "select report_count from glow_community_posts where id = ?",
            Long::class.java,
            postId,
        )!!

    private fun notificationCount(
        userId: Long,
        type: String,
        token: String,
    ): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from notifications where user_id = ? and type = ? and message like ?",
            Int::class.java,
            userId,
            type,
            "%$token%",
        )!!

    private fun member(role: UserRole = UserRole.USER): User {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val user = User(email = "gc$suffix@test.com", nickname = Nickname.of("g$suffix"), role = role)
        return userJpaRepository.save(user)
    }

    private fun rejected(action: () -> Unit): CustomException = assertThrows(CustomException::class.java) { action() }

    private fun token(): String = "t" + UUID.randomUUID().toString().replace("-", "").take(8)
}
