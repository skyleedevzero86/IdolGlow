package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("댓글 알림 대상 계산")
class ContentCommentNotificationPlannerTest {
    @DisplayName("답글이면 부모 작성자, 게시글이면 글쓴이에게 알리고 같은 사람은 한 번만 알린다")
    @Test
    fun replyAndOwnerWithoutDuplicates() {
        // given
        val actor = 1L

        // when
        val both = ContentCommentNotificationPlanner.plan(actor, 2L, 3L, emptySet())
        val same = ContentCommentNotificationPlanner.plan(actor, 2L, 2L, emptySet())

        // then
        assertEquals(
            listOf(
                CommentNotificationPlan(2L, NotificationType.REPLY_TO_MY_COMMENT),
                CommentNotificationPlan(3L, NotificationType.COMMENT_ON_MY_POST),
            ),
            both,
        )
        assertEquals(listOf(CommentNotificationPlan(2L, NotificationType.REPLY_TO_MY_COMMENT)), same)
    }

    @DisplayName("자기 자신과 관리자에게는 알리지 않고 관리자가 쓴 댓글은 아무에게도 알리지 않는다")
    @Test
    fun excludesSelfAndAdmins() {
        // given
        val adminIds = setOf(9L)

        // when
        val self = ContentCommentNotificationPlanner.plan(1L, parentAuthorId = 1L, ownerId = 1L, adminIds = adminIds)
        val toAdmin = ContentCommentNotificationPlanner.plan(1L, parentAuthorId = 9L, ownerId = 9L, adminIds = adminIds)
        val byAdmin = ContentCommentNotificationPlanner.plan(9L, parentAuthorId = 2L, ownerId = 3L, adminIds = adminIds)

        // then
        assertTrue(self.isEmpty())
        assertTrue(toAdmin.isEmpty())
        assertTrue(byAdmin.isEmpty())
    }

    @DisplayName("링크는 대상 경로 뒤에 댓글 위치를 붙이고 경로가 없으면 비운다")
    @Test
    fun linkFormat() {
        // given
        val path = "/glow?post=12"

        // when
        val link = ContentCommentNotificationPlanner.link(path, 34L)
        val missing = ContentCommentNotificationPlanner.link(null, 34L)

        // then
        assertEquals("/glow?post=12#comment-34", link)
        assertNull(missing)
        assertEquals("가나다라마바사아자차카타파하가나다라마바…", ContentCommentNotificationPlanner.titlePreview("가나다라마바사아자차카타파하가나다라마바사아"))
    }
}
