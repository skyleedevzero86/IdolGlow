package com.sleekydz86.idolglow.admin.adapter.web.dto

data class AdminSystemStatusResponse(
    val cpu: AdminCpuStatusResponse,
    val memory: AdminMemoryStatusResponse,
    val disk: AdminDiskStatusResponse,
    val jvm: AdminJvmStatusResponse,
)
