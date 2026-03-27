package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSlotResponse
import com.sleekydz86.idolglow.productpackage.admin.infrastructure.ProductOptionAdminRepository
import com.sleekydz86.idolglow.productpackage.admin.ui.request.CreateReservationSlotsRequest
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import com.sleekydz86.idolglow.productpackage.product.application.ProductCommandService
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
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
        require(!request.endDate.isBefore(request.startDate)) { "종료일은 시작일보다 빠를 수 없습니다." }
        require(request.startHour < request.endHour) { "시작 시간은 종료 시간보다 빨라야 합니다." }

        val product = productCommandRepository.findById(productId)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId")

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
            ?: throw IllegalArgumentException("예약 슬롯을 찾을 수 없습니다. slotId=$slotId")
        val now = LocalDateTime.now()
        slot.validateAvailability(slot.product.id, now)
        require(!reservationRepository.existsByReservationSlotId(slotId)) {
            "예약 이력이 있는 예약 슬롯은 삭제할 수 없습니다."
        }
        reservationSlotRepository.delete(slot)
    }

    fun deleteProduct(productId: Long) {
        require(!reservationRepository.existsByProductId(productId)) {
            "예약 이력이 있는 상품은 삭제할 수 없습니다."
        }
        productCommandService.deleteProduct(productId)
    }

    fun deleteOption(optionId: Long) {
        require(!productOptionAdminRepository.existsByOptionId(optionId)) {
            "상품에 연결된 옵션은 삭제할 수 없습니다."
        }
        val option = optionRepository.findById(optionId)
            ?: throw IllegalArgumentException("옵션을 찾을 수 없습니다. optionId=$optionId")
        imageEventPublisher.publishDelete(ImageAggregateType.OPTION, optionId)
        optionRepository.delete(option)
    }

    private fun slotKey(date: LocalDate, startTime: LocalTime): String =
        "$date|$startTime"
}
