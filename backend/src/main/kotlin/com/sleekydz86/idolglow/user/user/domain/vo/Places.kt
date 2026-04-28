package com.sleekydz86.idolglow.user.user.domain.vo

class Places private constructor(
    val value: List<String>
) {

    companion object {

        fun of(raw: List<String>): Places {
            val normalized = raw.asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
            return Places(normalized)
        }
    }

    fun applyTo(target: MutableList<String>) {
        target.clear()
        target.addAll(value)
    }
}
