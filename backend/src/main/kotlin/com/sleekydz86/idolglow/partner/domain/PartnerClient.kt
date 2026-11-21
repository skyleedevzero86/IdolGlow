package com.sleekydz86.idolglow.partner.domain

import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "partner_client",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_partner_client_key_hash", columnNames = ["key_hash"]),
    ],
)
class PartnerClient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false, length = 100)
    val name: String,
    @Column(name = "key_hash", nullable = false, length = 64)
    val keyHash: String,
    @Column(nullable = false, length = 100)
    val locales: String,
    @Column(name = "rate_limit_per_minute", nullable = false)
    val rateLimitPerMinute: Int,
    @Column(nullable = false)
    var enabled: Boolean,
) : BaseEntity() {
    fun contractedLocales(): List<String> = locales.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun serves(locale: String): Boolean = locale == EventLocale.KOREAN || locale in contractedLocales()

    fun defaultLocale(): String = contractedLocales().firstOrNull() ?: EventLocale.KOREAN

    fun fallbackOrder(locale: String): List<String> = EventLocale.order(locale).filter(::serves)

    fun changeEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    companion object {
        val SUPPORTED_LOCALES: List<String> = listOf("ko", "en", "ja", "zh")
        private const val NAME_MAX: Int = 100
        private const val MAX_RATE_LIMIT: Int = 10_000

        fun issue(
            name: String,
            keyHash: String,
            locales: List<String>,
            rateLimitPerMinute: Int,
        ): PartnerClient {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty() && trimmed.length <= NAME_MAX) { "파트너 이름은 1~${NAME_MAX}자로 입력해 주세요." }
            val normalized = locales.map(EventLocale::normalize).distinct()
            require(normalized.isNotEmpty()) { "계약 언어를 하나 이상 입력해 주세요." }
            require(normalized.all { it in SUPPORTED_LOCALES }) {
                "지원하지 않는 언어가 있습니다. 가능한 값: ${SUPPORTED_LOCALES.joinToString()}"
            }
            require(rateLimitPerMinute in 1..MAX_RATE_LIMIT) { "분당 호출 한도는 1~${MAX_RATE_LIMIT}회로 입력해 주세요." }
            return PartnerClient(
                name = trimmed,
                keyHash = keyHash,
                locales = normalized.joinToString(","),
                rateLimitPerMinute = rateLimitPerMinute,
                enabled = true,
            )
        }
    }
}
