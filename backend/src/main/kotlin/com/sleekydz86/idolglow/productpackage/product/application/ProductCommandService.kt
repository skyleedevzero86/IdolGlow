package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import com.sleekydz86.idolglow.productpackage.product.application.dto.CreateProductCommand
import com.sleekydz86.idolglow.productpackage.product.application.event.ProductCreateEvent
import com.sleekydz86.idolglow.productpackage.product.application.event.ProductLocationCommandService
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.productpackage.product.infrastructure.TourAttractionPicksJsonCodec
import com.sleekydz86.idolglow.wish.application.WishEventPublisher
import com.sleekydz86.idolglow.wish.domain.vo.WishAggregateType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@Transactional
@Service
class ProductCommandService(
    private val productCommandRepository: ProductCommandRepository,
    private val optionRepository: OptionRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val productLocationCommandService: ProductLocationCommandService,
    private val wishEventPublisher: WishEventPublisher,
    private val imageEventPublisher: ImageEventPublisher,
    private val objectMapper: ObjectMapper,
) {

    fun createProduct(command: CreateProductCommand): Product {
        val options = findOptions(command.optionIds)
        val today = LocalDate.now()
        val slotStartDate = command.slotStartDate ?: today.plusDays(1)
        val slotEndDate = command.slotEndDate ?: today.plusMonths(12)
        val product = Product.createWithTimeSlots(
            name = command.name,
            description = command.description,
            basePrice = command.basePrice,
            options = options,
            tagNames = command.tagNames,
            slotStartDate = slotStartDate,
            slotEndDate = slotEndDate,
            slotStartTime = command.slotStartTime,
            slotEndTime = command.slotEndTime
        )
        product.tourAttractionPicksJson =
            TourAttractionPicksJsonCodec.encode(command.tourAttractionPicks, objectMapper)
        val saved = productCommandRepository.save(product)

        eventPublisher.publishEvent(
            ProductCreateEvent(
                productId = saved.id,
                location = command.location
            )
        )

        return saved
    }

    fun updateProduct(
        productId: Long,
        command: CreateProductCommand,
    ): Product {
        val product = productCommandRepository.findById(productId)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId")
        val options = findOptions(command.optionIds)
        product.updateBasics(
            name = command.name,
            description = command.description,
            basePrice = command.basePrice,
        )
        product.replaceOptions(options)
        product.replaceTags(command.tagNames)
        product.tourAttractionPicksJson =
            TourAttractionPicksJsonCodec.encode(command.tourAttractionPicks, objectMapper)
        command.location?.let { payload ->
            productLocationCommandService.upsertProductLocation(productId, payload)
        }
        return productCommandRepository.save(product)
    }

    fun deleteProduct(productId: Long) {
        val product = productCommandRepository.findById(productId)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId")

        productCommandRepository.delete(product)

        wishEventPublisher.publishDelete(
            aggregateType = WishAggregateType.PRODUCT,
            aggregateId = productId
        )
        imageEventPublisher.publishDelete(
            aggregateType = ImageAggregateType.PRODUCT,
            aggregateId = productId
        )
    }

    private fun findOptions(optionIds: List<Long>): List<Option> {
        val distinctIds = optionIds.distinct()
        if (distinctIds.isEmpty()) {
            return emptyList()
        }
        val options = optionRepository.findAllByIdIn(distinctIds)
        require(options.size == distinctIds.size) { "옵션 중 일부를 찾을 수 없습니다." }
        val byId = options.associateBy { it.id }
        return distinctIds.map { optionId ->
            byId[optionId]
                ?: throw IllegalArgumentException("옵션을 찾을 수 없습니다. optionId=$optionId")
        }
    }
}
