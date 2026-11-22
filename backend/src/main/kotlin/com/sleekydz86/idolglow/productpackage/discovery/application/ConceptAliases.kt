package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType

object ConceptAliases {
    private val labels: Map<ConceptType, String> =
        mapOf(
            ConceptType.GIRL_CRUSH to "걸크러시",
            ConceptType.LOVELY_FRESH to "러블리, 청순",
            ConceptType.ELEGANT_GLAM to "우아, 글램",
            ConceptType.DREAMY to "몽환",
            ConceptType.HIGHTEEN to "하이틴",
        )

    private val whitespace = Regex("\\s+")
    private val separators = Regex("[/,]")

    fun labelOf(concept: ConceptType): String? = labels[concept]

    fun aliasesOf(concept: ConceptType): List<String> {
        val label = labels[concept] ?: return emptyList()
        return (listOf(concept.name, label) + label.split(separators).map { it.trim() })
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun matches(
        concept: ConceptType,
        text: String,
    ): Boolean {
        val keys = aliasFragments(concept)
        return keys.isNotEmpty() && fragments(text).any { it in keys }
    }

    fun resolve(text: String): ConceptType? = ConceptType.entries.firstOrNull { matches(it, text) }

    fun resolveName(name: String): ConceptType? = ConceptType.entries.firstOrNull { it.name == name }

    private fun aliasFragments(concept: ConceptType): Set<String> = aliasesOf(concept).flatMap(::fragments).toSet()

    private fun fragments(text: String): List<String> =
        text
            .lowercase()
            .split(separators)
            .map { it.replace(whitespace, "") }
            .filter { it.isNotEmpty() }
}
