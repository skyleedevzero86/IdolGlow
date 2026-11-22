package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "event_external_id",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_external_id_scheme_value", columnNames = ["scheme", "external_id"]),
    ],
)
class EventExternalId(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "global_id", nullable = false, length = 36)
    var globalId: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val scheme: ExternalIdScheme,
    @Column(name = "external_id", nullable = false, length = 80)
    val externalId: String,
) : BaseEntity() {
    fun moveTo(target: String) {
        globalId = target
    }

    companion object {
        fun manual(
            globalId: String,
            scheme: ExternalIdScheme,
            externalId: String,
        ): EventExternalId {
            require(scheme.manual) { "출처에서 자동으로 오는 외부 ID는 직접 붙일 수 없습니다. scheme=$scheme" }
            val value = externalId.trim()
            require(scheme.validFormat(value)) { "외부 ID 형식이 올바르지 않습니다. scheme=$scheme externalId=$value" }
            return EventExternalId(globalId = globalId, scheme = scheme, externalId = value)
        }
    }
}
