package com.sleekydz86.idolglow.eventinfo.domain

enum class ExternalIdScheme(
    val sources: Set<String>,
    val manual: Boolean,
) {
    KOPIS(setOf("KOPIS_PBLPRFR", "KOPIS_PRFFEST"), false),
    KTO(setOf("TOUR_API"), false),
    SJW(setOf("SEOUL_SJW_PERFORM"), false),
    CULTURE(setOf("CULTURE_INFO", "CULTURE_CALENDAR"), false),
    WIKIDATA(emptySet(), true),
    ;

    fun validFormat(externalId: String): Boolean = this != WIKIDATA || WIKIDATA_FORMAT.matches(externalId)

    companion object {
        private val WIKIDATA_FORMAT: Regex = Regex("^Q[1-9][0-9]{0,11}$")

        fun ofSource(source: String): ExternalIdScheme? = entries.firstOrNull { source in it.sources }

        fun parse(value: String): ExternalIdScheme =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("알 수 없는 외부 ID 체계입니다. scheme=$value")
    }
}
