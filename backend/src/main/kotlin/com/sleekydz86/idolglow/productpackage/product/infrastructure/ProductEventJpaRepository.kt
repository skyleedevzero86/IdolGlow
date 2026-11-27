package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ProductEventJpaRepository : JpaRepository<Product, Long> {
    @Query(
        """
        select p
        from Product p
        where p.eventSale.startsOn <= :today
          and p.eventSale.endsOn >= :today
        order by p.eventSale.endsOn asc, p.id desc
        """,
    )
    fun findActiveEvents(
        @Param("today") today: LocalDate,
        pageable: Pageable,
    ): List<Product>

    @Query(
        """
        select distinct p
        from Product p
        join p.productTags t
        where p.eventSale.startsOn <= :today
          and p.eventSale.endsOn >= :today
          and lower(t.tagName) = lower(:tag)
        order by p.eventSale.endsOn asc, p.id desc
        """,
    )
    fun findActiveEventsByTag(
        @Param("today") today: LocalDate,
        @Param("tag") tag: String,
        pageable: Pageable,
    ): List<Product>
}
