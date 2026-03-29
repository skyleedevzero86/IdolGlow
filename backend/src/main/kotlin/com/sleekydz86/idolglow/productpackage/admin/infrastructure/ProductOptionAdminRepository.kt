package com.sleekydz86.idolglow.productpackage.admin.infrastructure

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class ProductOptionAdminRepository(
    private val entityManager: EntityManager,
) {

    fun existsByOptionId(optionId: Long): Boolean =
        entityManager.createQuery(
            "select count(po) from ProductOption po where po.option.id = :optionId",
            java.lang.Long::class.java
        )
            .setParameter("optionId", optionId)
            .singleResult > 0
}