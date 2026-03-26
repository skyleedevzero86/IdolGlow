package com.sleekydz86.idolglow.wish.application

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class WishCommandService(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val wishRepository: WishRepository
) {

    fun toggle(userId: Long, productId: Long): WishToggleResponse {
        val user = loadUser(userId)
        val product = loadProduct(productId)
        val existing = wishRepository.findByUserIdAndProductId(userId, productId)

        return if (existing != null) {
            wishRepository.delete(existing)
            WishToggleResponse(existing.id, false)
        } else {
            val saved = wishRepository.save(Wish(user = user, product = product))
            WishToggleResponse(saved.id, true)
        }
    }

    private fun loadUser(userId: Long): User =
        userRepository.findById(userId)
            ?: throw EntityNotFoundException("User with id $userId does not exist.")

    private fun loadProduct(productId: Long): Product =
        productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product with id $productId does not exist.")
}
