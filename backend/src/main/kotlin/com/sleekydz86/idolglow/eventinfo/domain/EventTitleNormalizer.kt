package com.sleekydz86.idolglow.eventinfo.domain

object EventTitleNormalizer {
    private val GROUP = Regex("""\[[^]]*]|（[^）]*）|\([^)]*\)""")
    private val FILLER = Regex("""[\s\p{Punct}·ㆍ~～]+""")

    fun normalize(raw: String): String = raw.replace(GROUP, "").lowercase().replace(FILLER, "")
}
