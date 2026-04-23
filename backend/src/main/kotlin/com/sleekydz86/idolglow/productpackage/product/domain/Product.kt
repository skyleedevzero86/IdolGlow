package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.collections.asSequence

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val productOptions: MutableList<ProductOption> = mutableListOf(),

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val productTags: MutableList<ProductTag> = mutableListOf(),

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val reservationSlots: MutableList<ReservationSlot> = mutableListOf(),

    @OneToOne(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var productLocation: ProductLocation? = null,

    ) : BaseEntity() {

    @get:Transient
    val minPrice: BigDecimal
        get() = productOptions.asSequence()
            .map { it.option.price }
            .minOrNull()
            ?: BigDecimal.ZERO

    @get:Transient
    val totalPrice: BigDecimal
        get() = productOptions.fold(BigDecimal.ZERO) { totalPrice, productOption ->
            totalPrice + productOption.option.price
        }

    fun validateTotalPrice(requestedTotalPrice: BigDecimal) {
        if (totalPrice.compareTo(requestedTotalPrice) != 0) {
            throw IllegalArgumentException("상품 ${id}의 총금액이 일치하지 않습니다.")
        }
    }

    fun addOptions(options: Collection<Option>) {
        options.distinctBy { it.id }
            .forEach { option ->
                productOptions.add(ProductOption(product = this, option = option))
            }
    }

    fun replaceOptions(options: Collection<Option>) {
        val requestedById = options
            .asSequence()
            .distinctBy { it.id }
            .associateBy { it.id }

        if (requestedById.isEmpty()) {
            productOptions.clear()
            return
        }

        val existingOptionIds = productOptions
            .map { it.option.id }
            .toSet()

        productOptions.removeIf { productOption ->
            productOption.option.id !in requestedById
        }

        requestedById.forEach { (optionId, option) ->
            if (optionId !in existingOptionIds) {
                productOptions.add(ProductOption(product = this, option = option))
            }
        }
    }

    fun addTags(tagNames: Collection<String>) {
        tagNames.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { name ->
                productTags.add(ProductTag(product = this, tagName = name))
            }
    }

    fun replaceTags(tagNames: Collection<String>) {
        val normalizedRequested = tagNames.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        if (normalizedRequested.isEmpty()) {
            productTags.clear()
            return
        }

        val existing = productTags.map { it.tagName }.toSet()
        productTags.removeIf { it.tagName !in normalizedRequested }
        normalizedRequested
            .asSequence()
            .filter { it !in existing }
            .forEach { name -> productTags.add(ProductTag(product = this, tagName = name)) }
    }

    fun updateBasics(
        name: String,
        description: String,
    ) {
        require(name.isNotBlank()) { "상품명은 비어 있을 수 없습니다." }
        require(description.isNotBlank()) { "상품 설명은 비어 있을 수 없습니다." }
        this.name = name.trim()
        this.description = description.trim()
    }

    fun addReservationSlots(
        startDate: LocalDate,
        endDate: LocalDate,
        startTime: LocalTime = DEFAULT_RESERVATION_START_TIME,
        endTime: LocalTime = DEFAULT_RESERVATION_END_TIME
    ) {
        require(!endDate.isBefore(startDate)) { "종료일은 시작일보다 빠를 수 없습니다." }
        require(startTime < endTime) { "예약 시작 시간은 종료 시간보다 빨라야 합니다." }

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            var currentStartTime = startTime
            while (currentStartTime < endTime) {
                val currentEndTime = minOf(currentStartTime.plusHours(1), endTime)
                reservationSlots.add(
                    ReservationSlot(
                        product = this,
                        reservationDate = currentDate,
                        startTime = currentStartTime,
                        endTime = currentEndTime
                    )
                )
                currentStartTime = currentStartTime.plusHours(1)
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    fun setLocation(productLocation: ProductLocation) {
        require(productLocation.product === this)
        this.productLocation = productLocation
    }

    fun hasLocation(): Boolean = productLocation != null

    companion object {
        const val DEFAULT_RESERVATION_START_HOUR = 9
        const val DEFAULT_RESERVATION_END_HOUR = 16
        val DEFAULT_RESERVATION_START_TIME: LocalTime = LocalTime.of(DEFAULT_RESERVATION_START_HOUR, 0)
        val DEFAULT_RESERVATION_END_TIME: LocalTime = LocalTime.of(DEFAULT_RESERVATION_END_HOUR, 0)

        fun createWithTimeSlots(
            name: String,
            description: String,
            options: Collection<Option>,
            tagNames: Collection<String>,
            slotStartDate: LocalDate,
            slotEndDate: LocalDate,
            slotStartTime: LocalTime = DEFAULT_RESERVATION_START_TIME,
            slotEndTime: LocalTime = DEFAULT_RESERVATION_END_TIME
        ): Product {
            val product = Product(
                name = name,
                description = description
            )
            product.addOptions(options)
            product.addTags(tagNames)
            product.addReservationSlots(
                startDate = slotStartDate,
                endDate = slotEndDate,
                startTime = slotStartTime,
                endTime = slotEndTime
            )
            return product
        }
    }
}
