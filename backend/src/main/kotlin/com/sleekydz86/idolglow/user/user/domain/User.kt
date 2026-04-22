package com.sleekydz86.idolglow.user.user.domain

import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var email: String,

    @Embedded
    var nickname: Nickname,

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    var accountStatus: UserAccountStatus = UserAccountStatus.APPROVED,

    @Column(name = "platform_username", length = 50)
    var platformUsername: String? = null,

    @Column(name = "login_fail_count", nullable = false)
    var loginFailCount: Int = 0,

    @Column(name = "account_locked_at")
    var accountLockedAt: LocalDateTime? = null,

    @Column(name = "password_changed_at")
    var passwordChangedAt: LocalDateTime? = null,

    @Column(name = "last_password_change_date")
    var lastPasswordChangeDate: LocalDate? = null,

    @Column(name = "password_change_daily_count", nullable = false)
    var passwordChangeDailyCount: Int = 0,

    @Column
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "temporary_password_required", nullable = false)
    var temporaryPasswordRequired: Boolean = false,

    @Column(name = "temporary_password_issued_at")
    var temporaryPasswordIssuedAt: LocalDateTime? = null,
) {
    fun updateLastLoginTime(now: LocalDateTime = LocalDateTime.now()) {
        lastLoginAt = now
    }

    fun updateNickname(nickname: String) {
        this.nickname = Nickname.of(nickname)
    }

    fun changeRole(role: UserRole) {
        this.role = role
    }

    fun isPlatformActive(): Boolean {
        val statusOk = accountStatus == UserAccountStatus.APPROVED
        val notLocked = !isPlatformLocked()
        return statusOk && notLocked
    }

    fun isPlatformLocked(): Boolean =
        loginFailCount >= 5 || accountStatus == UserAccountStatus.SUSPENDED

    fun applyEncodedPasswordChange(encodedPassword: String) {
        passwordHash = encodedPassword
        passwordChangedAt = LocalDateTime.now()
        val today = LocalDate.now()
        if (lastPasswordChangeDate == null || lastPasswordChangeDate != today) {
            passwordChangeDailyCount = 1
            lastPasswordChangeDate = today
        } else {
            passwordChangeDailyCount += 1
        }
    }

    fun increasePlatformLoginFailCount() {
        loginFailCount += 1
        if (loginFailCount >= 5) {
            accountStatus = UserAccountStatus.SUSPENDED
            accountLockedAt = LocalDateTime.now()
            log.warn("로그인 실패로 계정이 잠겼습니다. userId={}, loginFailCount={}", id, loginFailCount)
        }
    }

    fun resetPlatformLoginFailCount() {
        loginFailCount = 0
        accountLockedAt = null
    }

    fun approvePlatform(approverId: Long) {
        accountStatus = UserAccountStatus.APPROVED
        accountLockedAt = null
        loginFailCount = 0
        log.info("플랫폼 계정이 승인되었습니다. id={}, approverId={}", id, approverId)
    }

    fun changeAccountStatus(status: UserAccountStatus) {
        accountStatus = status
        if (status != UserAccountStatus.SUSPENDED) {
            accountLockedAt = null
        }
        if (status == UserAccountStatus.APPROVED) {
            loginFailCount = 0
        }
    }

    fun issueTemporaryPassword(encodedPassword: String, now: LocalDateTime = LocalDateTime.now()) {
        passwordHash = encodedPassword
        temporaryPasswordRequired = true
        temporaryPasswordIssuedAt = now
    }

    fun completePasswordChange(encodedPassword: String, now: LocalDateTime = LocalDateTime.now()) {
        passwordHash = encodedPassword
        passwordChangedAt = now
        temporaryPasswordRequired = false
        temporaryPasswordIssuedAt = null
    }

    fun unlockAccount() {
        loginFailCount = 0
        accountLockedAt = null
        if (accountStatus == UserAccountStatus.SUSPENDED) {
            accountStatus = UserAccountStatus.APPROVED
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(User::class.java)

        fun of(
            email: String,
            nickname: String = "",
            role: UserRole = UserRole.USER,
        ): User {
            val resolvedNickname = if (nickname.isBlank()) Nickname.defaultFromEmail(email) else Nickname.of(nickname)
            return User(
                email = email,
                nickname = resolvedNickname,
                profileImageUrl = null,
                passwordHash = null,
                role = role,
            )
        }
    }
}
