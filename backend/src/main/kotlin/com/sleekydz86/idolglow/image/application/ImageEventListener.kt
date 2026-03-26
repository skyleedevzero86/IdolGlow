package com.sleekydz86.idolglow.image.application

import com.sleekydz86.idolglow.image.domain.ImageRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ImageEventListener(
    private val imageRepository: ImageRepository,
    private val imageStorage: ImageStorage
) {

    @Async("imageStorageExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleImageCreate(event: ImageCreateEvent) {
        require(event.content.isNotEmpty()) { "content must not be empty." }

        val image = Image.createAndStore(
            aggregateType = event.aggregateType,
            aggregateId = event.aggregateId,
            originalFilename = event.originalFilename,
            content = event.content,
            sortOrder = event.sortOrder,
            imageStorage = imageStorage,
        )
        imageRepository.save(image)
    }

    @Async("imageStorageExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleImageDelete(event: ImageDeleteEvent) {
        imageRepository.deleteAllByAggregate(
            aggregateType = event.aggregateType,
            aggregateId = event.aggregateId,
        )
    }
}
