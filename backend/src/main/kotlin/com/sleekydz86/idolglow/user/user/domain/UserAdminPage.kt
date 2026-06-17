package com.sleekydz86.idolglow.user.user.domain

import com.sleekydz86.idolglow.user.user.domain.vo.UserRole

data class UserAdminPage(
    val items: List<User>,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)
