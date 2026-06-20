package com.sleekydz86.idolglow.admin.adapter.web.mapper

import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminUserPageResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminUserSummaryResponse
import com.sleekydz86.idolglow.admin.application.dto.AdminUserPageResult
import com.sleekydz86.idolglow.admin.application.dto.AdminUserSummaryResult
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import java.time.format.DateTimeFormatter

private val adminUserDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

fun AdminUserPageResult.toWebResponse(): AdminUserPageResponse =
    AdminUserPageResponse(
        users = users.map { it.toWebResponse() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
        hasNext = hasNext,
        totalUsers = totalUsers,
        adminCount = adminCount,
        suspendedCount = suspendedCount,
        withdrawnCount = withdrawnCount,
    )

fun AdminUserSummaryResult.toWebResponse(): AdminUserSummaryResponse =
    AdminUserSummaryResponse(
        id = id,
        email = email,
        nickname = nickname,
        role = role.name,
        roleLabel = role.toLabel(),
        accountStatus = accountStatus.name,
        accountStatusLabel = accountStatus.toLabel(),
        loginFailCount = loginFailCount,
        locked = locked,
        platformUsername = platformUsername,
        profileImageUrl = profileImageUrl,
        lastLoginAt = lastLoginAt?.format(adminUserDateTimeFormatter),
        oauthLinked = oauthLinked,
        oauthProviders = oauthProviders,
    )

private fun UserRole.toLabel(): String =
    when (this) {
        UserRole.USER -> "일반회원"
        UserRole.ADMIN -> "관리자"
    }

private fun UserAccountStatus.toLabel(): String =
    when (this) {
        UserAccountStatus.PENDING -> "대기"
        UserAccountStatus.APPROVED -> "승인"
        UserAccountStatus.REJECTED -> "거절"
        UserAccountStatus.SUSPENDED -> "정지"
        UserAccountStatus.WITHDRAWN -> "탈퇴"
    }
