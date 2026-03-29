package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import com.sleekydz86.idolglow.productpackage.product.application.dto.CreateProductCommand
import com.sleekydz86.idolglow.productpackage.product.application.event.ProductCreateEvent
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.wish.application.WishEventPublisher
import com.sleekydz86.idolglow.wish.domain.vo.WishAggregateType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional
@Service
class ProductCommandService(
    private val productCommandRepository: ProductCommandRepository,
    private val optionRepository: OptionRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val wishEventPublisher: WishEventPublisher,
    private val imageEventPublisher: ImageEventPublisher,
) {

    fun createProduct(command: CreateProductCommand): Product {
        val options = findOptions(command.optionIds)
        val today = LocalDate.now()
        val slotStartDate = command.slotStartDate ?: today.plusDays(1)
        val slotEndDate = command.slotEndDate ?: today.plusMonths(12)
        val product = Product.createWithTimeSlots(
            name = command.name,
            description = command.description,
            options = options,
            tagNames = command.tagNames,
            slotStartDate = slotStartDate,
            slotEndDate = slotEndDate,
            slotStartHour = command.slotStartHour,
            slotEndHour = command.slotEndHour
        )
        val saved = productCommandRepository.save(product)

        eventPublisher.publishEvent(
            ProductCreateEvent(
                productId = saved.id,
                location = command.location
            )
        )

        return saved
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
        val options = optionRepository.findAllByIdIn(optionIds)
        require(options.size == optionIds.size) { "옵션 중 일부를 찾을 수 없습니다." }
        return options
    }
}
