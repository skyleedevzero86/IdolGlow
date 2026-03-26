package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<Product, Long>
