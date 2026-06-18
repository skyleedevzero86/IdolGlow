package com.sleekydz86.idolglow.admin.adapter.web.mapper

import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminActuatorMetricResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminActuatorStatusResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminCpuStatusResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminDiskStatusResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminInfrastructureStatusResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminJvmStatusResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminMemoryStatusResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminServerStatusResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminServerSummaryResponse
import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminSystemStatusResponse
import com.sleekydz86.idolglow.admin.application.dto.AdminActuatorMetricResult
import com.sleekydz86.idolglow.admin.application.dto.AdminActuatorStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminCpuStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminDiskStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminInfrastructureStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminJvmStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminMemoryStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminServerStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminServerSummaryResult
import com.sleekydz86.idolglow.admin.application.dto.AdminSystemStatusResult

fun AdminServerStatusResult.toWebResponse(): AdminServerStatusResponse =
    AdminServerStatusResponse(
        generatedAt = generatedAt,
        overallStatus = overallStatus,
        summary = summary.toWebResponse(),
        system = system.toWebResponse(),
        infrastructure = infrastructure.map { it.toWebResponse() },
        actuator = actuator.toWebResponse(),
    )

fun AdminServerSummaryResult.toWebResponse(): AdminServerSummaryResponse =
    AdminServerSummaryResponse(
        cpuUsagePercent = cpuUsagePercent,
        memoryUsagePercent = memoryUsagePercent,
        diskUsagePercent = diskUsagePercent,
        jvmHeapUsagePercent = jvmHeapUsagePercent,
        uptimeSeconds = uptimeSeconds,
    )

fun AdminSystemStatusResult.toWebResponse(): AdminSystemStatusResponse =
    AdminSystemStatusResponse(
        cpu = cpu.toWebResponse(),
        memory = memory.toWebResponse(),
        disk = disk.toWebResponse(),
        jvm = jvm.toWebResponse(),
    )

fun AdminCpuStatusResult.toWebResponse(): AdminCpuStatusResponse =
    AdminCpuStatusResponse(
        systemUsagePercent = systemUsagePercent,
        processUsagePercent = processUsagePercent,
        systemLoadAverage = systemLoadAverage,
        availableProcessors = availableProcessors,
    )

fun AdminMemoryStatusResult.toWebResponse(): AdminMemoryStatusResponse =
    AdminMemoryStatusResponse(
        totalBytes = totalBytes,
        freeBytes = freeBytes,
        usedBytes = usedBytes,
        maxBytes = maxBytes,
        usagePercent = usagePercent,
    )

fun AdminDiskStatusResult.toWebResponse(): AdminDiskStatusResponse =
    AdminDiskStatusResponse(
        mountPath = mountPath,
        fileStoreName = fileStoreName,
        totalBytes = totalBytes,
        freeBytes = freeBytes,
        usedBytes = usedBytes,
        usagePercent = usagePercent,
    )

fun AdminJvmStatusResult.toWebResponse(): AdminJvmStatusResponse =
    AdminJvmStatusResponse(
        heapUsedBytes = heapUsedBytes,
        heapCommittedBytes = heapCommittedBytes,
        heapMaxBytes = heapMaxBytes,
        heapUsagePercent = heapUsagePercent,
        nonHeapUsedBytes = nonHeapUsedBytes,
        nonHeapCommittedBytes = nonHeapCommittedBytes,
        liveThreadCount = liveThreadCount,
        daemonThreadCount = daemonThreadCount,
        peakThreadCount = peakThreadCount,
        uptimeSeconds = uptimeSeconds,
        startTime = startTime,
    )

fun AdminInfrastructureStatusResult.toWebResponse(): AdminInfrastructureStatusResponse =
    AdminInfrastructureStatusResponse(
        type = type,
        label = label,
        status = status,
        message = message,
        responseTimeMs = responseTimeMs,
        details = details,
    )

fun AdminActuatorStatusResult.toWebResponse(): AdminActuatorStatusResponse =
    AdminActuatorStatusResponse(
        enabled = enabled,
        healthEndpoint = healthEndpoint,
        metricsEndpoint = metricsEndpoint,
        metrics = metrics.map { it.toWebResponse() },
    )

fun AdminActuatorMetricResult.toWebResponse(): AdminActuatorMetricResponse =
    AdminActuatorMetricResponse(
        name = name,
        value = value,
    )
