package com.sleekydz86.idolglow.history.application

import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.history.application.dto.RecordContentViewCommand
import com.sleekydz86.idolglow.history.application.port.`in`.ContentViewUseCase
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@DisplayName("최근 본 콘텐츠")
class ContentViewPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var contentViewUseCase: ContentViewUseCase

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("같은 콘텐츠는 한 줄만 남기고 다시 보면 제목과 본 시각이 바뀌어 맨 앞에 온다")
    @Test
    fun upsertKeepsOneRowAndNewestFirst() {
        // given
        val me = member()
        record(me, ContentTargetType.PRODUCT, "15", "성수 팝업", "/products/15")
        record(me, ContentTargetType.SUBWAY, "2", "2호선", "/subway/2")

        // when
        record(me, ContentTargetType.PRODUCT, "15", "성수 팝업 투어", "/products/15?tab=review")
        val page = contentViewUseCase.findMine(me.id, OffsetPageRequest.of(0, 20))

        // then
        assertEquals(2L, page.totalElements)
        assertEquals(listOf("PRODUCT", "SUBWAY"), page.items.map { it.contentType })
        assertEquals("성수 팝업 투어", page.items.first().title)
        assertEquals("/products/15?tab=review", page.items.first().path)
    }

    @DisplayName("제목이 비면 경로를 제목으로 쓰고 관리자 경로는 거절한다")
    @Test
    fun blankTitleUsesPathAndAdminPathRejected() {
        // given
        val me = member()

        // when
        val saved = record(me, ContentTargetType.GLOW_MAP, "map", " ", "/glow/map")
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                record(me, ContentTargetType.OTHER, "admin", "관리", "/admin/users")
            }

        // then
        assertEquals("/glow/map", saved.title)
        assertTrue(error.message!!.contains("관리자"))
    }

    @DisplayName("본인 기록만 하나씩 지울 수 있고 전체 삭제는 내 기록만 지운다")
    @Test
    fun ownOnlyDeleteAndClear() {
        // given
        val me = member()
        val other = member()
        val mine = record(me, ContentTargetType.ARTICLE, "a1", "기사", "/articles/a1")
        val others = record(other, ContentTargetType.ARTICLE, "a1", "기사", "/articles/a1")

        // when
        val error = assertThrows(EntityNotFoundException::class.java) { contentViewUseCase.delete(me.id, others.id) }
        contentViewUseCase.delete(me.id, mine.id)
        record(me, ContentTargetType.ARCHIVE, "x", "아카이브", "/archive/x")
        contentViewUseCase.clear(me.id)

        // then
        assertTrue(error.message!!.contains("${others.id}"))
        assertEquals(0L, contentViewUseCase.findMine(me.id, OffsetPageRequest.of(0, 20)).totalElements)
        assertEquals(1L, contentViewUseCase.findMine(other.id, OffsetPageRequest.of(0, 20)).totalElements)
    }

    @DisplayName("대표 이미지 주소를 저장하고 다시 보면 바꾸며 안전하지 않은 주소는 버린다")
    @Test
    fun imageUrlRoundTrip() {
        // given
        val me = member()
        recordImage(me, ContentTargetType.ARTICLE, "img", "https://cdn.example.com/a.jpg")

        // when
        val updated = recordImage(me, ContentTargetType.ARTICLE, "img", "/uploads/idolglow/b.png")
        val unsafe = recordImage(me, ContentTargetType.PRODUCT, "js", "javascript:alert(1)")
        val page = contentViewUseCase.findMine(me.id, OffsetPageRequest.of(0, 20))

        // then
        assertEquals("/uploads/idolglow/b.png", updated.imageUrl)
        assertEquals(null, unsafe.imageUrl)
        assertEquals(listOf(null, "/uploads/idolglow/b.png"), page.items.map { it.imageUrl })
    }

    @DisplayName("회원당 최근 200개만 남기고 오래된 기록부터 지운다")
    @Test
    fun keepsAtMostTwoHundred() {
        // given
        val me = member()
        jdbcTemplate.update(
            """
            insert into user_content_views (user_id, content_type, content_key, title, path, viewed_at, created_at, updated_at)
            select ?, 'OTHER', 'old-' || g, '옛 기록 ' || g, '/old/' || g,
                   now() - (g || ' minutes')::interval, now(), now()
            from generate_series(1, 200) as g
            """.trimIndent(),
            me.id,
        )

        // when
        record(me, ContentTargetType.EXCHANGE_RATE, "usd", "환율", "/exchange-rate")
        val page = contentViewUseCase.findMine(me.id, OffsetPageRequest.of(0, 1))

        // then
        assertEquals(200L, page.totalElements)
        assertEquals("EXCHANGE_RATE", page.items.single().contentType)
        assertEquals(
            0,
            jdbcTemplate.queryForObject(
                "select count(*) from user_content_views where user_id = ? and content_key = 'old-200'",
                Int::class.java,
                me.id,
            ),
        )
    }

    private fun record(
        user: User,
        type: ContentTargetType,
        key: String,
        title: String?,
        path: String,
    ) = contentViewUseCase.record(RecordContentViewCommand(user.id, type, key, title, path))

    private fun recordImage(
        user: User,
        type: ContentTargetType,
        key: String,
        imageUrl: String,
    ) = contentViewUseCase.record(RecordContentViewCommand(user.id, type, key, "제목", "/contents/$key", imageUrl))

    private fun member(): User {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return userJpaRepository.save(User(email = "cv$suffix@test.com", nickname = Nickname.of("v$suffix")))
    }
}
