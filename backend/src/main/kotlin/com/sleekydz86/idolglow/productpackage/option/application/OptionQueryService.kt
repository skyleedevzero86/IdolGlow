package com.sleekydz86.idolglow.productpackage.option.application

import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionResponse
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class OptionQueryService(
    private val optionRepository: OptionRepository,
) {

    fun findOptions(): List<OptionResponse> =
        optionRepository.findAll()
            .map { OptionResponse.from(it) }

    fun findOption(optionId: Long): OptionResponse =
        optionRepository.findById(optionId)
            ?.let { OptionResponse.from(it) }
            ?: throw IllegalArgumentException("Option with id $optionId does not exist.")
}
