package com.sleekydz86.idolglow.admin.ui.request

import com.sleekydz86.idolglow.user.user.domain.vo.UserRole

data class UpdateAdminUserRoleRequest(
    val role: UserRole,
)
