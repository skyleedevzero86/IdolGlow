package com.sleekydz86.idolglow.user.user.infrastructure

import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository
): UserRepository {

    override fun findById(userId: Long): User? =
        userJpaRepository.findByIdOrNull(userId)

    override fun findByEmail(email: String): User? =
        userJpaRepository.findByEmail(email)

    override fun findByNicknameValue(nickname: String): User? =
        userJpaRepository.findByNicknameValue(nickname)

    override fun save(user: User): User =
        userJpaRepository.save(user)

    override fun saveAndFlush(user: User): User =
        userJpaRepository.saveAndFlush(user)
}