package com.sleekydz86.idolglow.luckydraw.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "lucky_draw_admin_locks")
class LuckyDrawAdminLock(
    @Id
    val id: Long = SINGLE_ID,
) {
    companion object {
        const val SINGLE_ID = 1L
    }
}
