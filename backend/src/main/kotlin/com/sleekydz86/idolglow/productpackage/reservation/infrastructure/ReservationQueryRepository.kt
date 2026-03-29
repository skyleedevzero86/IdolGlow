package com.sleekydz86.idolglow.productpackage.reservation.infrastructure

import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class ReservationQueryRepository(
    private val entityManager: EntityManager
) {

    fun findByUserId(userId: Long): List<Reservation> =
        entityManager.createQuery(
            """
            select distinct r from Reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product p
            left join fetch p.productOptions po
            left join fetch po.option o
            where r.userId = :userId
            order by r.visitDate desc, r.visitStartTime desc
            """.trimIndent(),
            Reservation::class.java
        ).setParameter("userId", userId)
            .resultList

    fun findByIdAndUserId(reservationId: Long, userId: Long): Reservation? =
        entityManager.createQuery(
            """
            select distinct r from Reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product p
            left join fetch p.productOptions po
            left join fetch po.option o
            where r.id = :reservationId and r.userId = :userId
            """.trimIndent(),
            Reservation::class.java
        ).setParameter("reservationId", reservationId)
            .setParameter("userId", userId)
            .resultList
            .firstOrNull()
}
