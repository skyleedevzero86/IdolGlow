package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin.MACHINE
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin.MANUAL
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin.PROVIDER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("공연 번역 덮어쓰기: 사람 > 공식 > AI")
class EventTranslationOfferTest {
    @DisplayName("새 출처가 더 높거나, 같고 AI가 아니면 바꾸고, 아니면 그대로 둔다")
    @ParameterizedTest(name = "{1}이(가) {0} 위에 오면 바뀜={2}")
    @CsvSource(
        "MACHINE, PROVIDER, true",
        "PROVIDER, PROVIDER, true",
        "MANUAL, PROVIDER, false",
        "MACHINE, MACHINE, false",
        "PROVIDER, MACHINE, false",
        "MANUAL, MACHINE, false",
        "MACHINE, MANUAL, true",
        "PROVIDER, MANUAL, true",
        "MANUAL, MANUAL, true",
    )
    fun offerFollowsPriority(
        existing: EventTranslationOrigin,
        incoming: EventTranslationOrigin,
        replaced: Boolean,
    ) {
        // given
        val row = EventTranslation(globalId = "g-1", locale = "ja", title = "旧タイトル", origin = existing)

        // when
        val changed = row.offer(incoming, "新タイトル", null)

        // then
        assertEquals(replaced, changed)
        assertEquals(if (replaced) incoming else existing, row.origin)
        assertEquals(if (replaced) "新タイトル" else "旧タイトル", row.title)
    }

    @DisplayName("같은 출처가 같은 제목·줄거리를 다시 주면 바뀐 것으로 보지 않는다")
    @Test
    fun sameTextIsNotAChange() {
        // given
        val row = EventTranslation.provider("g-1", "ja", "同じ", "あらすじ")

        // when
        val changed = row.offer(PROVIDER, "同じ", "あらすじ")

        // then
        assertEquals(false, changed)
    }

    @DisplayName("사람 번역 제목이 255자를 넘으면 받지 않고, 255자는 받는다")
    @Test
    fun manualTitleLengthBoundary() {
        // given
        val exact = "訳".repeat(EventTranslation.TITLE_MAX)
        val over = "訳".repeat(EventTranslation.TITLE_MAX - 1) + "😀"

        // when
        val accepted = EventTranslation.manual("g-1", "ja", exact)
        val rejected = runCatching { EventTranslation.manual("g-1", "zh", over + "国") }

        // then
        assertEquals(MANUAL, accepted.origin)
        assertEquals(EventTranslation.TITLE_MAX, accepted.title.codePointCount(0, accepted.title.length))
        assertEquals("공연 번역 제목은 255자 이하여야 합니다. locale=zh", rejected.exceptionOrNull()?.message)
    }

    @DisplayName("한국어는 번역 언어가 아니라서 사람 번역으로 받지 않는다")
    @Test
    fun manualRejectsKorean() {
        // when
        val rejected = runCatching { EventTranslation.manual("g-1", "ko", "한국어 제목") }

        // then
        assertEquals("번역 언어는 en, ja, zh 중 하나여야 합니다. locale=ko", rejected.exceptionOrNull()?.message)
    }

    @DisplayName("AI 번역도 화면에 보이는 출처이고, 지표용 검수 번역 목록에는 들어가지 않는다")
    @Test
    fun machineIsServedButNotReviewed() {
        // then
        assertEquals(listOf(PROVIDER, MANUAL), EventTranslationOrigin.REVIEWED)
        assertEquals(MACHINE, EventTranslation.machine("g-1", "en", "Title (제목)").origin)
    }
}
