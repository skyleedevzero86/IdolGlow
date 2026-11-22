package com.sleekydz86.idolglow.global.support

import java.util.Locale

enum class ContentTargetType {
    ARTICLE,
    PRODUCT,
    GLOW,
    GLOW_COMMUNITY_POST,
    EVENT_INFO,
    ARCHIVE,
    SUBWAY,
    AIRPORT_CROWD,
    EXCHANGE_RATE,
    GLOW_WEATHER,
    GLOW_MAP,
    OTHER,
    ;

    companion object {
        private const val ECHO_LIMIT = 40
        private val BLOCKED = setOf("NOTICE", "EVENT")

        fun parse(raw: String?): ContentTargetType {
            val normalized =
                raw
                    ?.trim()
                    ?.uppercase(Locale.ROOT)
                    ?.replace('-', '_')
                    .orEmpty()
            require(normalized.isNotEmpty()) { "콘텐츠 유형을 입력해 주세요." }
            require(normalized !in BLOCKED) { "공지사항과 이벤트에는 사용할 수 없는 기능입니다." }
            return entries.firstOrNull { it.name == normalized }
                ?: throw IllegalArgumentException("지원하지 않는 콘텐츠 유형입니다: ${normalized.take(ECHO_LIMIT)}")
        }
    }
}

object ContentTargetPath {
    const val MAX_LENGTH = 500
    private const val ADMIN_PREFIX = "/admin"
    private val FORBIDDEN_CHARACTERS = Regex("[\\s\\\\<>\"]")

    fun normalize(raw: String?): String? {
        val path = raw?.trim().orEmpty()
        if (path.isEmpty()) return null
        require(path.length <= MAX_LENGTH) { "경로는 ${MAX_LENGTH}자 이하여야 합니다." }
        require(path.startsWith("/") && !path.startsWith("//")) { "경로는 / 로 시작하는 사이트 내부 경로여야 합니다." }
        require(!FORBIDDEN_CHARACTERS.containsMatchIn(path)) { "경로에 사용할 수 없는 문자가 있습니다." }
        require(!path.lowercase(Locale.ROOT).startsWith(ADMIN_PREFIX)) { "관리자 경로는 사용할 수 없습니다." }
        return path
    }

    fun requirePath(raw: String?): String = normalize(raw) ?: throw IllegalArgumentException("경로를 입력해 주세요.")
}
