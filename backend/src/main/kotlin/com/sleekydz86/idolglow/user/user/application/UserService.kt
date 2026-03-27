package com.sleekydz86.idolglow.user.user.application

import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.domain.UserOAuthRepository
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userOAuthRepository: UserOAuthRepository,
) {

    fun getUser(userId: Long): GetUserLoginInfoResponse {
        val user = findUser(userId)
        return GetUserLoginInfoResponse.from(user, findPreferredOAuthProfile(user.id))
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId)
            ?: throw CustomException(AuthExceptionType.USER_NOT_FOUND)

    @Transactional
    fun updateNickname(userId: Long, nickname: String): GetUserLoginInfoResponse {
        val user = findUser(userId)
        user.updateNickname(nickname)
        return GetUserLoginInfoResponse.from(userRepository.save(user), findPreferredOAuthProfile(user.id))
    }

    private fun findPreferredOAuthProfile(userId: Long) =
        userOAuthRepository.findAllByUserId(userId)
            .sortedWith(compareByDescending { it.provider == AuthProvider.GOOGLE })
            .firstOrNull { !it.profileName.isNullOrBlank() || !it.profileImageUrl.isNullOrBlank() }
}
