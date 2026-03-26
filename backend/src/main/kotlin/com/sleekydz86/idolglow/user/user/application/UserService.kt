package com.sleekydz86.idolglow.user.user.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getUser(userId: Long): GetUserLoginInfoResponse {
        val user = findUser(userId)
        return GetUserLoginInfoResponse.from(user)
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId)
            ?: throw CustomException(AuthExceptionType.USER_NOT_FOUND)

    @Transactional
    fun updateNickname(userId: Long, nickname: String): GetUserLoginInfoResponse {
        val user = findUser(userId)
        user.updateNickname(nickname)
        return GetUserLoginInfoResponse.from(userRepository.save(user))
    }
}
