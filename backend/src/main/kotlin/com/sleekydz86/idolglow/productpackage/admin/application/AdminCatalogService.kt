package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import com.sleekydz86.idolglow.productpackage.product.application.ProductCommandService
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.collections.map

@Transactional
@Service
class AdminCatalogService(
    private val productCommandRepository: ProductCommandRepository,
    private val productCommandService: ProductCommandService,
    private val optionRepository: OptionRepository,
    private val productOptionAdminRepository: ProductOptionAdminRepository,
    private val reservationRepository: ReservationRepository,
    private val reservationSlotRepository: ReservationSlotRepository,
    private val imageEventPublisher: ImageEventPublisher,
) {

    fun findSlots(productId: Long): List<AdminReservationSlotResponse> =
        reservationSlotRepository.findAllByProductId(productId)
            .map(AdminReservationSlotResponse::from)

    fun createSlots(productId: Long, request: CreateReservationSlotsRequest): List<AdminReservationSlotResponse> {
        require(!request.endDate.isBefore(request.startDate)) { "endDate must not be before startDate." }
        require(request.startHour < request.endHour) { "startHour must be before endHour." }

        val product = productCommandRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")

        val existingKeys = product.reservationSlots
            .map { slotKey(it.reservationDate, it.startTime) }
            .toMutableSet()

        var currentDate = request.startDate
        while (!currentDate.isAfter(request.endDate)) {
            for (hour in request.startHour until request.endHour) {
                val startTime = LocalTime.of(hour, 0)
                val key = slotKey(currentDate, startTime)
                if (key !in existingKeys) {
                    product.reservationSlots.add(
                        ReservationSlot(
                            product = product,
                            reservationDate = currentDate,
                            startTime = startTime,
                            endTime = startTime.plusHours(1)
                        )
                    )
                    existingKeys += key
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        productCommandRepository.save(product)
        return findSlots(productId)
    }

    fun deleteSlot(slotId: Long) {
        val slot = reservationSlotRepository.findByIdForUpdate(slotId)
            ?: throw IllegalArgumentException("Reservation slot not found: $slotId")
        val now = LocalDateTime.now()
        slot.validateAvailability(slot.product.id, now)
        require(!reservationRepository.existsByReservationSlotId(slotId)) {
            "Reservation slot with reservation history cannot be deleted."
        }
        reservationSlotRepository.delete(slot)
    }

    fun deleteProduct(productId: Long) {
        require(!reservationRepository.existsByProductId(productId)) {
            "Product with reservation history cannot be deleted."
        }
        productCommandService.deleteProduct(productId)
    }

    fun deleteOption(optionId: Long) {
        require(!productOptionAdminRepository.existsByOptionId(optionId)) {
            "Option attached to a product cannot be deleted."
        }
        val option = optionRepository.findById(optionId)
            ?: throw IllegalArgumentException("Option not found: $optionId")
        imageEventPublisher.publishDelete(ImageAggregateType.OPTION, optionId)
        optionRepository.delete(option)
    }

    private fun slotKey(date: LocalDate, startTime: LocalTime): String =
        "$date|$startTime"
}
