package com.sleekydz86.idolglow.productpackage.option.infrastructure

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import org.springframework.data.jpa.repository.JpaRepository

interface OptionJpaRepository : JpaRepository<Option, Long> {
    fun findAllByIdIn(optionIds: List<Long>): List<Option>
}