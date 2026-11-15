package com.sleekydz86.idolglow.translation.domain

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
    name = "translation_report",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_translation_report_user", columnNames = ["translation_id", "user_id"]),
    ],
)
class TranslationReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "translation_id", nullable = false)
    val translationId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : BaseEntity()
