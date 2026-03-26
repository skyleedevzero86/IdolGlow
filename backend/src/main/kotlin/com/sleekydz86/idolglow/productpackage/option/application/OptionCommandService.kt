package com.sleekydz86.idolglow.productpackage.option.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
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
