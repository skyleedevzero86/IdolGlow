package com.sleekydz86.idolglow.partner.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "partner_call_log")
class PartnerCallLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "partner_id", nullable = false)
    val partnerId: Long,
    @Column(nullable = false, length = 200)
    val path: String,
    @Column(nullable = false)
    val status: Int,
    @Column(name = "item_count", nullable = false)
    val itemCount: Int,
    @Column(name = "called_at", nullable = false)
    val calledAt: LocalDateTime,
) : BaseEntity()
