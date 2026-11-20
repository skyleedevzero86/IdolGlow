package com.sleekydz86.idolglow.partner.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@DisplayName("파트너 계약 언어·키·커서")
class PartnerDomainTest {
    @DisplayName("계약 언어와 한국어 원문만 제공하고, 대체 순서도 그 안에서만 고른다")
    @Test
    fun servesOnlyContractedLocalesAndKorean() {
        // given
        val client = PartnerClient.issue("서울 여행사", "hash", listOf("ja"), 60)

        // when
        val order = client.fallbackOrder("ja")

        // then
        assertTrue(client.serves("ja"))
        assertTrue(client.serves("ko"))
        assertFalse(client.serves("en"))
        assertEquals(listOf("ja", "ko"), order)
        assertEquals("ja", client.defaultLocale())
    }

    @DisplayName("지원하지 않는 언어로는 발급하지 않는다")
    @Test
    fun rejectsUnsupportedLocale() {
        // when
        val error = assertThrows<IllegalArgumentException> { PartnerClient.issue("여행사", "hash", listOf("fr"), 60) }

        // then
        assertEquals("지원하지 않는 언어가 있습니다. 가능한 값: ko, en, ja, zh", error.message)
    }

    @DisplayName("키 원문은 매번 다르고 해시는 64자로 같은 키면 같다")
    @Test
    fun generatesKeysAndStableHash() {
        // given
        val first = PartnerKey.generate()
        val second = PartnerKey.generate()

        // when
        val hash = PartnerKey.hash(first)

        // then
        assertNotEquals(first, second)
        assertTrue(first.startsWith("pk_"))
        assertEquals(64, hash.length)
        assertEquals(hash, PartnerKey.hash(" $first "))
    }

    @DisplayName("커서는 수정 시각과 글로벌 ID를 그대로 되돌린다")
    @Test
    fun cursorRoundTrips() {
        // given
        val cursor = PartnerFeedCursor(LocalDateTime.of(2026, 11, 21, 9, 0, 0, 123_456_000), "gid-1")

        // when
        val decoded = PartnerFeedCursor.decode(cursor.encode())

        // then
        assertEquals(cursor, decoded)
    }

    @DisplayName("읽을 수 없는 커서는 400으로 알린다")
    @Test
    fun rejectsBrokenCursor() {
        // when
        val error = assertThrows<IllegalArgumentException> { PartnerFeedCursor.decode("not-a-cursor") }

        // then
        assertEquals("커서 형식이 올바르지 않습니다.", error.message)
    }

    @DisplayName("시간대가 붙은 시각은 서버 시간대로 바꿔 시작점으로 쓴다")
    @Test
    fun sinceConvertsOffsetToServerZone() {
        // when
        val start = PartnerFeedCursor.since("2026-11-21T00:00:00Z")

        // then
        val expected = LocalDateTime.ofInstant(Instant.parse("2026-11-21T00:00:00Z"), ZoneId.systemDefault())
        assertEquals(expected, start.updatedAt)
        assertEquals("", start.globalId)
    }
}
