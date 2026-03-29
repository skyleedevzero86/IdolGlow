package com.sleekydz86.idolglow.productpackage.option.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.productpackage.option.application.dto.CreateOptionCommand
import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionImageFile
import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionResponse
import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class OptionCommandService(
    private val optionRepository: OptionRepository,
    private val imageEventPublisher: ImageEventPublisher,
) {

    fun createOption(
        command: CreateOptionCommand,
        imageFiles: List<OptionImageFile> = emptyList(),
    ): OptionResponse {
        val option = Option(
            name = command.name,
            description = command.description,
            price = command.price,
            location = command.location
        )
        val saved = optionRepository.save(option)

        imageFiles
            .filter { it.content.isNotEmpty() }
            .forEach { image ->
                imageEventPublisher.publishCreate(
                    aggregateType = ImageAggregateType.OPTION,
                    aggregateId = saved.id,
                    originalFilename = image.originalFilename,
                    content = image.content,
                    sortOrder = image.sortOrder
                )
            }

        return OptionResponse.from(saved)
    }
}
