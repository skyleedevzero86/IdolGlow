package com.sleekydz86.idolglow.support

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Component
class PostgresReservationFixture(
    private val userJpaRepository: UserJpaRepository,
    private val productCommandRepository: ProductCommandRepository,
    private val reservationCommandService: ReservationCommandService,
) {
    fun persistOpenSlot(price: BigDecimal = BigDecimal("10000")): OpenSlot {
        val visitDate = LocalDate.now().plusDays(3)
        val product =
            productCommandRepository.save(
                Product.createWithTimeSlots(
                    name = "슬롯${UUID.randomUUID().toString().take(8)}",
                    description = "설명",
                    basePrice = price,
                    options = emptyList(),
                    tagNames = emptyList(),
                    slotStartDate = visitDate,
                    slotEndDate = visitDate,
                ),
            )
        val slot = product.reservationSlots.first()
        return OpenSlot(productId = product.id, slotId = slot.id, totalPrice = product.totalPrice)
    }

    fun persistUser(): User {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        return userJpaRepository.save(
            User(
                email = "u$token@test.com",
                nickname = Nickname.of("u$token"),
            ),
        )
    }

    fun createReservation(
        userId: Long,
        slot: OpenSlot,
    ) = reservationCommandService.createReservation(
        CreateReservationCommand(
            productId = slot.productId,
            reservationSlotId = slot.slotId,
            userId = userId,
            totalPrice = slot.totalPrice,
        ),
    )

    data class OpenSlot(
        val productId: Long,
        val slotId: Long,
        val totalPrice: BigDecimal,
    )
}
