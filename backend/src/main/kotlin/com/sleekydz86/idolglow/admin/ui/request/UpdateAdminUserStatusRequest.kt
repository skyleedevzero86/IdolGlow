package com.sleekydz86.idolglow.admin.ui.request

import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus

data class UpdateAdminUserStatusRequest(
    val accountStatus: UserAccountStatus,
)
