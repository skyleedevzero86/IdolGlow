package com.sleekydz86.idolglow.admin.ui.dto

data class AdminSystemStatusResponse(
    val cpu: AdminCpuStatusResponse,
    val memory: AdminMemoryStatusResponse,
    val disk: AdminDiskStatusResponse,
    val jvm: AdminJvmStatusResponse,
)
