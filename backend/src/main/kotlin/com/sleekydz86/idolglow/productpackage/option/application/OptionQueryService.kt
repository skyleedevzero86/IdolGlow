package com.sleekydz86.idolglow.productpackage.option.application

import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionPageResponse
import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionResponse
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
            ?: throw IllegalArgumentException("옵션을 찾을 수 없습니다. optionId=$optionId")

    fun searchOptions(
        q: String?,
        page: Int,
        size: Int,
    ): OptionPageResponse {
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, 50),
            Sort.by(Sort.Direction.ASC, "id"),
        )
        return OptionPageResponse.from(optionRepository.findWithSearch(q, pageable))
    }
}
