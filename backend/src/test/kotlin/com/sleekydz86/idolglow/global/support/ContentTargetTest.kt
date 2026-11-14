package com.sleekydz86.idolglow.global.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("콘텐츠 대상 유형과 경로")
class ContentTargetTest {
    @DisplayName("공지사항과 이벤트는 대상 유형으로 받지 않는다")
    @Test
    fun rejectsNoticeAndEvent() {
        // given
        val blocked = listOf("NOTICE", "event", " Notice ")

        // when
        val errors =
            blocked.map { raw ->
                assertThrows(IllegalArgumentException::class.java) { ContentTargetType.parse(raw) }
            }

        // then
        assertTrue(errors.all { it.message == "공지사항과 이벤트에는 사용할 수 없는 기능입니다." })
        assertEquals(ContentTargetType.GLOW_COMMUNITY_POST, ContentTargetType.parse("glow-community-post"))
        assertThrows(IllegalArgumentException::class.java) { ContentTargetType.parse("UNKNOWN") }
    }

    @DisplayName("경로는 / 로 시작하는 내부 경로만 받고 관리자 경로는 거절한다")
    @Test
    fun pathMustBeInternalAndNotAdmin() {
        // given
        val unsafe = listOf("/admin", "/ADMIN/users", "https://evil.example", "//evil.example", "glow", "/a b")

        // when
        val errors = unsafe.map { raw -> runCatching { ContentTargetPath.normalize(raw) }.exceptionOrNull() }

        // then
        assertTrue(errors.all { it is IllegalArgumentException })
        assertEquals("/glow?post=12", ContentTargetPath.normalize(" /glow?post=12 "))
        assertNull(ContentTargetPath.normalize(" "))
        assertThrows(IllegalArgumentException::class.java) { ContentTargetPath.requirePath(null) }
    }
}
