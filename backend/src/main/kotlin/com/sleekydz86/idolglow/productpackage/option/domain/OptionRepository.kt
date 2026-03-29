package com.sleekydz86.idolglow.productpackage.option.domain

interface OptionRepository {
    fun findAllByIdIn(optionIds: List<Long>): List<Option>
    fun findAll(): List<Option>
    fun findById(optionId: Long): Option?
    fun save(option: Option): Option
    fun delete(option: Option)
}