package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import java.time.format.DateTimeFormatter

internal val adminUserDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

internal fun UserRole.label(): String =
    when (this) {
        UserRole.USER -> "일반회원"
        UserRole.ADMIN -> "관리자"
    }

internal fun UserAccountStatus.label(): String =
    when (this) {
        UserAccountStatus.PENDING -> "대기"
        UserAccountStatus.APPROVED -> "승인"
        UserAccountStatus.REJECTED -> "거절"
        UserAccountStatus.SUSPENDED -> "정지"
        UserAccountStatus.WITHDRAWN -> "탈퇴"
    }
