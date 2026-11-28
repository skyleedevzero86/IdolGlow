package com.sleekydz86.idolglow.survey.domain.autopick

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "survey_auto_pick_products",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_survey_auto_pick_products_setting_product",
            columnNames = ["setting_id", "product_id"],
        ),
    ],
)
class SurveyAutoPickProduct(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "setting_id", nullable = false)
    val settingId: Long,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
)
