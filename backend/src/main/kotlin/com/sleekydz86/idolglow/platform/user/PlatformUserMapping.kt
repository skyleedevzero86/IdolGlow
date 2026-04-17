package com.sleekydz86.idolglow.platform.user

import com.sleekydz86.idolglow.platform.user.domain.PlatformUser
import com.sleekydz86.idolglow.platform.user.domain.PlatformUserRole
import com.sleekydz86.idolglow.platform.user.domain.PlatformUserStatus
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole

fun UserAccountStatus.toPlatformUserStatus(): PlatformUserStatus =
    PlatformUserStatus.valueOf(name)

fun PlatformUserStatus.toUserAccountStatus(): UserAccountStatus =
    UserAccountStatus.valueOf(name)

fun UserRole.toPlatformUserRole(): PlatformUserRole =
    when (this) {
        UserRole.USER -> PlatformUserRole.USER
        UserRole.ADMIN -> PlatformUserRole.ADMIN
    }

fun PlatformUserRole.toUserRole(): UserRole =
    when (this) {
        PlatformUserRole.USER -> UserRole.USER
        PlatformUserRole.ADMIN -> UserRole.ADMIN
    }

fun User.toPlatformUser(): PlatformUser {
    val resolvedUsername = platformUsername ?: nickname.value
    return PlatformUser(
        id = if (id > 0L) id else null,
        username = resolvedUsername,
        password = passwordHash ?: "",
        nickname = nickname.value,
        email = email,
        status = accountStatus.toPlatformUserStatus(),
        role = role.toPlatformUserRole(),
        lastLoginAt = lastLoginAt,
        loginFailCount = loginFailCount,
        accountLockedAt = accountLockedAt,
        passwordChangedAt = passwordChangedAt,
        passwordChangeCount = passwordChangeDailyCount,
        lastPasswordChangeDate = lastPasswordChangeDate,
    )
}

fun PlatformUser.applyToEntity(entity: User) {
    entity.email = email
    entity.nickname = Nickname.of(nickname)
    entity.passwordHash = password
    entity.platformUsername = username
    entity.accountStatus = status.toUserAccountStatus()
    entity.role = role.toUserRole()
    entity.lastLoginAt = lastLoginAt
    entity.loginFailCount = loginFailCount
    entity.accountLockedAt = accountLockedAt
    entity.passwordChangedAt = passwordChangedAt
    entity.passwordChangeDailyCount = passwordChangeCount
    entity.lastPasswordChangeDate = lastPasswordChangeDate
}
