package com.sleekydz86.idolglow.user.user.application

import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.UserExceptionType
import com.sleekydz86.idolglow.global.exceptions.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.domain.UserOAuthRepository
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userOAuthRepository: UserOAuthRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun getUser(userId: Long): GetUserLoginInfoResponse {
        val user = findUser(userId)
        return buildLoginResponse(user)
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId)
            ?: throw CustomException(AuthExceptionType.USER_NOT_FOUND)

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String): Boolean {
        val user = findUser(userId)
        val hash = user.passwordHash
            ?: throw CustomException(UserExceptionType.PASSWORD_CHANGE_NOT_SUPPORTED)
        val current = currentPassword.trim()
        val next = newPassword.trim()
        if (!passwordEncoder.matches(current, hash)) {
            throw CustomException(UserExceptionType.CURRENT_PASSWORD_INCORRECT)
        }
        validatePasswordStrength(next)
        val wasTemporary = user.temporaryPasswordRequired
        val encoded = requireNotNull(passwordEncoder.encode(next)) { "비밀번호 인코딩에 실패했습니다." }
        user.completePasswordChange(encoded)
        userRepository.save(user)
        return wasTemporary
    }

    private fun validatePasswordStrength(password: String) {
        if (password.length < 8 || password.length > 72) {
            throw CustomException(UserExceptionType.SIGNUP_PASSWORD_POLICY)
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            throw CustomException(UserExceptionType.SIGNUP_PASSWORD_POLICY)
        }
    }

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
        return buildLoginResponse(userRepository.save(user))
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

    private fun buildLoginResponse(user: User): GetUserLoginInfoResponse {
        val rows = userOAuthRepository.findAllByUserId(user.id)
        if (rows.isEmpty()) {
            return GetUserLoginInfoResponse.from(user, false, null, null)
        }
        val sorted = rows.sortedWith(compareByDescending { it.provider == AuthProvider.GOOGLE })
        val primary = sorted.first()
        val oauthProfileName = primary.profileName?.trim()?.takeIf { it.isNotEmpty() }
        val pictureRow = sorted.firstOrNull { !it.profileImageUrl.isNullOrBlank() || !it.profileName.isNullOrBlank() }
        val profileImageFallback = pictureRow?.profileImageUrl?.trim()?.takeIf { it.isNotEmpty() }
        return GetUserLoginInfoResponse.from(user, true, oauthProfileName, profileImageFallback)
    }
}
