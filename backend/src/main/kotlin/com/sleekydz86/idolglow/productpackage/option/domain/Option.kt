package com.sleekydz86.idolglow.productpackage.option.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "options")
class Option(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false)
    var price: BigDecimal,

    @Column(nullable = false, length = 200)
    var location: String,
) : BaseEntity() {

    fun update(
        name: String,
        description: String,
        price: BigDecimal,
        location: String,
    ) {
        require(name.isNotBlank()) { "옵션명은 비어 있을 수 없습니다." }
        require(description.isNotBlank()) { "옵션 설명은 비어 있을 수 없습니다." }
        require(price >= BigDecimal.ZERO) { "옵션 가격은 0 이상이어야 합니다. (0은 상품/패키지 기본가만 적용, 추가 요금 없음)" }
        require(location.isNotBlank()) { "옵션 장소는 비어 있을 수 없습니다." }
        this.name = name.trim()
        this.description = description.trim()
        this.price = price
        this.location = location.trim()
    }
}
