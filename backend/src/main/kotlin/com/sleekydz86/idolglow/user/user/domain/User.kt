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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Column
    var lastLoginAt: LocalDateTime? = null
) {
    fun updateLastLoginTime(now : LocalDateTime = LocalDateTime.now()) {
        this.lastLoginAt = now
    }

    fun updateNickname(nickname: String) {
        this.nickname = Nickname.of(nickname)
    }

    fun changeRole(role: UserRole) {
        this.role = role
    }

    companion object {
        fun of(
            email: String,
            nickname: String = "",
            role: UserRole = UserRole.USER
        ): User {
            nickname.trim()
            val resolvedNickname = if (nickname.isBlank()) Nickname.defaultFromEmail(email) else Nickname.of(nickname)
            return User(email = email, nickname = resolvedNickname, role = role)
        }
    }
}
