package com.sleekydz86.idolglow.notification.application.port.out

interface AdminRecipientPort {
    fun findAdminUserIds(): List<Long>
}
