package com.sleekydz86.idolglow.productpackage.option.infrastructure

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface OptionJpaRepository : JpaRepository<Option, Long>, JpaSpecificationExecutor<Option> {
    fun findAllByIdIn(optionIds: List<Long>): List<Option>
}