package com.sleekydz86.idolglow.user.user.application

import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.UserExceptionType
import com.sleekydz86.idolglow.global.exceptions.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.domain.UserOAuthRepository
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI

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
    fun updateProfile(userId: Long, nickname: String?, profileImageUrl: String?): GetUserLoginInfoResponse {
        if (nickname == null && profileImageUrl == null) {
            return getUser(userId)
        }
        val user = findUser(userId)
        if (nickname != null) {
            user.updateNickname(nickname)
        }
        if (profileImageUrl != null) {
            applyProfileImageUrl(user, profileImageUrl)
        }
        return GetUserLoginInfoResponse.from(userRepository.save(user), findPreferredOAuthProfile(user.id))
    }

    private fun applyProfileImageUrl(user: User, raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) {
            user.profileImageUrl = null
            return
        }
        if (t.length > 500) {
            throw CustomException(UserExceptionType.INVALID_PROFILE_IMAGE_URL)
        }
        val uri = runCatching { URI.create(t) }.getOrElse {
            throw CustomException(UserExceptionType.INVALID_PROFILE_IMAGE_URL)
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw CustomException(UserExceptionType.INVALID_PROFILE_IMAGE_URL)
        }
        user.profileImageUrl = t
    }

    private fun findPreferredOAuthProfile(userId: Long) =
        userOAuthRepository.findAllByUserId(userId)
            .sortedWith(compareByDescending { it.provider == AuthProvider.GOOGLE })
            .firstOrNull { !it.profileName.isNullOrBlank() || !it.profileImageUrl.isNullOrBlank() }
}
