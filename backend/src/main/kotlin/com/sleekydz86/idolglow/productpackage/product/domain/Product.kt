package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.global.BaseEntity
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
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

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

    fun addTags(tagNames: Collection<String>) {
        tagNames.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { name ->
                productTags.add(ProductTag(product = this, tagName = name))
            }
    }

    fun addReservationSlots(
        startDate: LocalDate,
        endDate: LocalDate,
        startHour: Int = DEFAULT_RESERVATION_START_HOUR,
        endHour: Int = DEFAULT_RESERVATION_END_HOUR
    ) {
        require(!endDate.isBefore(startDate)) { "종료일은 시작일보다 빠를 수 없습니다." }
        require(
            startHour in DEFAULT_RESERVATION_START_HOUR..(DEFAULT_RESERVATION_END_HOUR - 1) &&
                    endHour in (DEFAULT_RESERVATION_START_HOUR + 1)..DEFAULT_RESERVATION_END_HOUR &&
                    startHour < endHour
        ) { "예약 가능 시간은 09시부터 16시 사이여야 합니다." }

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            for (hour in startHour until endHour) {
                val startTime = LocalTime.of(hour, 0)
                val endTime = startTime.plusHours(1)
                reservationSlots.add(
                    ReservationSlot(
                        product = this,
                        reservationDate = currentDate,
                        startTime = startTime,
                        endTime = endTime
                    )
                )
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

        fun createWithTimeSlots(
            name: String,
            description: String,
            options: Collection<Option>,
            tagNames: Collection<String>,
            slotStartDate: LocalDate,
            slotEndDate: LocalDate,
            slotStartHour: Int = DEFAULT_RESERVATION_START_HOUR,
            slotEndHour: Int = DEFAULT_RESERVATION_END_HOUR
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
                startHour = slotStartHour,
                endHour = slotEndHour
            )
            return product
        }
    }
}
