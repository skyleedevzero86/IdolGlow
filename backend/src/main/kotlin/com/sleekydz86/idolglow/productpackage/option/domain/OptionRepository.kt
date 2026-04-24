package com.sleekydz86.idolglow.productpackage.option.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface OptionRepository {
    fun findAllByIdIn(optionIds: List<Long>): List<Option>
    fun findAll(): List<Option>
    fun findById(optionId: Long): Option?
    fun findWithSearch(
        q: String?,
        pageable: Pageable,
    ): Page<Option>
    fun save(option: Option): Option
    fun delete(option: Option)
}