package com.sleekydz86.idolglow.survey.domain.autopick

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal

@Entity
@Table(
    name = "survey_auto_pick_settings",
    uniqueConstraints = [UniqueConstraint(name = "uk_survey_auto_pick_settings_user", columnNames = ["user_id"])],
)
class SurveyAutoPickSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false)
    var enabled: Boolean = false,
    @Column(name = "max_price", nullable = false, precision = 15, scale = 2)
    var maxPrice: BigDecimal,
) : BaseEntity() {
    fun update(
        enabled: Boolean,
        maxPrice: BigDecimal,
    ) {
        require(maxPrice.signum() >= 0) { "예산 상한은 0 이상이어야 합니다." }
        this.enabled = enabled
        this.maxPrice = maxPrice
    }
}
