package com.sleekydz86.idolglow.admin.adapter.web.request

import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus

data class UpdateAdminUserStatusRequest(
    val accountStatus: UserAccountStatus,
)
