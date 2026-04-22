package com.sleekydz86.idolglow.platform.user.domain

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class PlatformUser(
    var id: Long? = null,
    var username: String,
    var password: String,
    var nickname: String,
    var email: String,
    var status: PlatformUserStatus = PlatformUserStatus.PENDING,
    var role: PlatformUserRole = PlatformUserRole.USER,
    var lastLoginAt: LocalDateTime? = null,
    var loginFailCount: Int = 0,
    var accountLockedAt: LocalDateTime? = null,
    var passwordChangedAt: LocalDateTime? = null,
    var passwordChangeCount: Int = 0,
    var lastPasswordChangeDate: LocalDate? = null,
) {

    fun changePassword(encodedPassword: String) {
        password = encodedPassword
        passwordChangedAt = LocalDateTime.now()
        val today = LocalDate.now()
        if (lastPasswordChangeDate == null || lastPasswordChangeDate != today) {
            passwordChangeCount = 1
            lastPasswordChangeDate = today
        } else {
            passwordChangeCount = (passwordChangeCount) + 1
        }
    }

    fun canChangePassword(): Boolean {
        val today = LocalDate.now()
        if (lastPasswordChangeDate == null || lastPasswordChangeDate != today) {
            return true
        }
        val maxDailyChanges = 3
        return passwordChangeCount < maxDailyChanges
    }

    fun getTodayPasswordChangeCount(): Int {
        val today = LocalDate.now()
        if (lastPasswordChangeDate == null || lastPasswordChangeDate != today) {
            return 0
        }
        return passwordChangeCount
    }

    fun isPasswordChangeRequired(): Boolean {
        if (passwordChangedAt == null) {
            return true
        }
        return passwordChangedAt!!.isBefore(LocalDateTime.now().minusDays(30))
    }

    fun isPasswordChangeRecommended(): Boolean {
        if (passwordChangedAt == null) {
            return true
        }
        return passwordChangedAt!!.isBefore(LocalDateTime.now().minusDays(14))
    }

    fun isActive(): Boolean {
        val statusOk = status == PlatformUserStatus.APPROVED
        val notLocked = !isLocked()
        return statusOk && notLocked
    }

    fun isLocked(): Boolean =
        loginFailCount >= 5 || status == PlatformUserStatus.SUSPENDED

    fun increaseLoginFailCount() {
        loginFailCount += 1
        if (loginFailCount >= 5) {
            status = PlatformUserStatus.SUSPENDED
            accountLockedAt = LocalDateTime.now()
            log.warn("계정이 잠겼습니다: userId={}, loginFailCount={}", id, loginFailCount)
        }
    }

    fun resetLoginFailCount() {
        loginFailCount = 0
        accountLockedAt = null
    }

    fun updateLastLoginAt(lastLoginAt: LocalDateTime) {
        this.lastLoginAt = lastLoginAt
    }

    fun approve(approverId: Long) {
        status = PlatformUserStatus.APPROVED
        accountLockedAt = null
        loginFailCount = 0
        log.info("사용자가 승인되었습니다: id={}, approverId={}", id, approverId)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlatformUser::class.java)

        fun builder(): Builder = Builder()

        class Builder {
            private var id: Long? = null
            private var username: String? = null
            private var password: String? = null
            private var nickname: String? = null
            private var email: String? = null
            private var status: PlatformUserStatus = PlatformUserStatus.PENDING
            private var role: PlatformUserRole = PlatformUserRole.USER
            private var lastLoginAt: LocalDateTime? = null
            private var loginFailCount: Int = 0
            private var accountLockedAt: LocalDateTime? = null
            private var passwordChangedAt: LocalDateTime? = null
            private var passwordChangeCount: Int = 0
            private var lastPasswordChangeDate: LocalDate? = null

            fun id(id: Long?) = apply { this.id = id }
            fun username(username: String) = apply { this.username = username }
            fun password(password: String) = apply { this.password = password }
            fun nickname(nickname: String) = apply { this.nickname = nickname }
            fun email(email: String) = apply { this.email = email }
            fun status(status: PlatformUserStatus) = apply { this.status = status }
            fun role(role: PlatformUserRole) = apply { this.role = role }
            fun lastLoginAt(lastLoginAt: LocalDateTime?) = apply { this.lastLoginAt = lastLoginAt }
            fun loginFailCount(loginFailCount: Int) = apply { this.loginFailCount = loginFailCount }
            fun accountLockedAt(accountLockedAt: LocalDateTime?) = apply { this.accountLockedAt = accountLockedAt }
            fun passwordChangedAt(passwordChangedAt: LocalDateTime?) = apply { this.passwordChangedAt = passwordChangedAt }
            fun passwordChangeCount(passwordChangeCount: Int) = apply { this.passwordChangeCount = passwordChangeCount }
            fun lastPasswordChangeDate(lastPasswordChangeDate: LocalDate?) =
                apply { this.lastPasswordChangeDate = lastPasswordChangeDate }

            fun build(): PlatformUser = PlatformUser(
                id = id,
                username = username ?: error("사용자명(로그인 ID)이 필요합니다."),
                password = password ?: error("비밀번호가 필요합니다."),
                nickname = nickname ?: error("닉네임이 필요합니다."),
                email = email ?: error("이메일이 필요합니다."),
                status = status,
                role = role,
                lastLoginAt = lastLoginAt,
                loginFailCount = loginFailCount,
                accountLockedAt = accountLockedAt,
                passwordChangedAt = passwordChangedAt,
                passwordChangeCount = passwordChangeCount,
                lastPasswordChangeDate = lastPasswordChangeDate,
            )
        }
    }
}
