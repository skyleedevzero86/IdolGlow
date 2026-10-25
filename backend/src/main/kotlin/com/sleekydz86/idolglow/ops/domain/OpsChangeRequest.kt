package com.sleekydz86.idolglow.ops.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "ops_change_request")
class OpsChangeRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false, length = 80)
    val type: String,
    @Column(nullable = false, length = 255)
    val target: String,
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    val payload: String,
    @Column(name = "requested_by", nullable = false, length = 80)
    val requestedBy: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by_kind", nullable = false, length = 20)
    val requestedByKind: OpsActorKind,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OpsChangeStatus,
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "before_snapshot")
    var beforeSnapshot: String? = null,
    @Column(name = "approved_by", length = 80)
    var approvedBy: String? = null,
    @Column(name = "applied_at")
    var appliedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun approve(approverId: Long) {
        require(status == OpsChangeStatus.REQUESTED) {
            "승인할 수 없는 상태입니다. status=$status requestId=$id"
        }
        require(requestedByKind != OpsActorKind.USER || requestedBy != approverId.toString()) {
            "자기가 만든 요청은 승인할 수 없습니다. requestId=$id"
        }
        status = OpsChangeStatus.APPROVED
        approvedBy = approverId.toString()
    }

    fun reject() {
        require(status == OpsChangeStatus.REQUESTED) {
            "거절할 수 없는 상태입니다. status=$status requestId=$id"
        }
        status = OpsChangeStatus.REJECTED
    }

    fun markApplied(
        snapshot: String,
        appliedAt: LocalDateTime,
    ) {
        require(status == OpsChangeStatus.APPROVED) {
            "적용할 수 없는 상태입니다. status=$status requestId=$id"
        }
        require(snapshot.isNotBlank()) { "적용 전 상태가 비어 있습니다. requestId=$id" }
        beforeSnapshot = snapshot
        status = OpsChangeStatus.APPLIED
        this.appliedAt = appliedAt
    }

    fun markReverted() {
        require(status == OpsChangeStatus.APPLIED) {
            "되돌릴 수 없는 상태입니다. status=$status requestId=$id"
        }
        require(!beforeSnapshot.isNullOrBlank()) { "되돌릴 이전 상태가 없습니다. requestId=$id" }
        status = OpsChangeStatus.REVERTED
    }

    companion object {
        private const val TYPE_MAX: Int = 80
        private const val TARGET_MAX: Int = 255
        private const val PAYLOAD_MAX: Int = 8000
        private const val ACTOR_MAX: Int = 80

        fun open(
            type: String,
            target: String,
            payload: String,
            requestedBy: String,
            requestedByKind: OpsActorKind,
        ): OpsChangeRequest {
            require(type.isNotBlank()) { "운영 변경 종류는 비울 수 없습니다." }
            require(target.isNotBlank()) { "운영 변경 대상은 비울 수 없습니다. type=$type" }
            require(payload.isNotBlank()) { "운영 변경 내용은 비울 수 없습니다. target=$target" }
            require(requestedBy.isNotBlank()) { "요청자는 비울 수 없습니다. target=$target" }
            return OpsChangeRequest(
                type = type.trim().take(TYPE_MAX),
                target = target.trim().take(TARGET_MAX),
                payload = payload.trim().take(PAYLOAD_MAX),
                requestedBy = requestedBy.trim().take(ACTOR_MAX),
                requestedByKind = requestedByKind,
                status = OpsChangeStatus.REQUESTED,
            )
        }
    }
}
